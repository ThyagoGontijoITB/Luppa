package com.luppa.api.service;

import com.luppa.api.dto.ReportCategoryResponse;
import com.luppa.api.dto.ReportCreateRequest;
import com.luppa.api.dto.ReportResponse;
import com.luppa.api.dto.ReportStatusUpdateRequest;
import com.luppa.api.entity.Event;
import com.luppa.api.entity.Rating;
import com.luppa.api.entity.Report;
import com.luppa.api.entity.ReportCategory;
import com.luppa.api.entity.ReportStatus;
import com.luppa.api.entity.StatusReport;
import com.luppa.api.entity.User;
import com.luppa.api.entity.UserRole;
import com.luppa.api.exception.ForbiddenOperationException;
import com.luppa.api.exception.InvalidRequestException;
import com.luppa.api.exception.ResourceNotFoundException;
import com.luppa.api.mapper.ReportMapper;
import com.luppa.api.repository.EventRepository;
import com.luppa.api.repository.RatingRepository;
import com.luppa.api.repository.ReportCategoryRepository;
import com.luppa.api.repository.ReportRepository;
import com.luppa.api.repository.StatusReportRepository;
import com.luppa.api.security.AuthUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReportService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReportService.class);

    private final ReportRepository reportRepository;
    private final ReportCategoryRepository reportCategoryRepository;
    private final EventRepository eventRepository;
    private final RatingRepository ratingRepository;
    private final StatusReportRepository statusReportRepository;
    private final AccountEmailService emailService;

    public ReportService(
            ReportRepository reportRepository,
            ReportCategoryRepository reportCategoryRepository,
            EventRepository eventRepository,
            RatingRepository ratingRepository,
            StatusReportRepository statusReportRepository,
            AccountEmailService emailService
    ) {
        this.reportRepository = reportRepository;
        this.reportCategoryRepository = reportCategoryRepository;
        this.eventRepository = eventRepository;
        this.ratingRepository = ratingRepository;
        this.statusReportRepository = statusReportRepository;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    public List<ReportCategoryResponse> categories() {
        return reportCategoryRepository.findAll().stream()
                .map(ReportMapper::toCategoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> findAll() {
        return reportRepository.findAll().stream()
                .map(ReportMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> findByEvent(Integer eventId) {
        return reportRepository.findByEventIdOrderByReportedAtDesc(eventId).stream()
                .map(ReportMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> findMine(AuthUser authUser) {
        return reportRepository.findByReporterIdOrderByReportedAtDesc(authUser.idUsuario()).stream()
                .map(ReportMapper::toResponse)
                .toList();
    }

    @Transactional
    public ReportResponse create(Integer eventId, ReportCreateRequest request, AuthUser authUser) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        ReportCategory category = reportCategoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Report category not found"));
        Report report = new Report();
        report.setEvent(event);
        report.setReporter(authUser.user());
        report.setCategory(category);
        report.setStatus(status(0));
        report.setDescription(request.description().trim());
        if (request.ratingId() != null) {
            Rating rating = ratingRepository.findById(request.ratingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Rating not found"));
            if (!rating.getEvent().getId().equals(eventId)) {
                throw new InvalidRequestException("Rating does not belong to this event");
            }
            report.setRating(rating);
        }
        return ReportMapper.toResponse(reportRepository.save(report));
    }

    @Transactional
    public ReportResponse updateStatus(Integer reportId, ReportStatusUpdateRequest request, AuthUser authUser) {
        if (!authUser.user().getRole().getName().isAdminLike()) {
            throw new ForbiddenOperationException("Only admins can update report status");
        }
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        var previousStatus = report.getStatus().getStatus();
        String responseMessage = request.responseMessage() == null ? "" : request.responseMessage().trim();
        if (requiresModerationResponse(request.status()) && responseMessage.isBlank()) {
            throw new InvalidRequestException("Moderation response is required for resolved or dismissed reports");
        }
        report.setStatus(status(StatusReport.idFor(request.status())));
        if (requiresModerationResponse(request.status())) {
            report.setModerationResponse(responseMessage);
            report.setModerationRespondedAt(LocalDateTime.now());
        }
        if (previousStatus != request.status()) {
            scheduleModerationEmail(report, request.status(), responseMessage);
        }
        return ReportMapper.toResponse(report);
    }

    private boolean requiresModerationResponse(ReportStatus status) {
        return status == ReportStatus.RESOLVED || status == ReportStatus.DISMISSED;
    }

    private void scheduleModerationEmail(Report report, ReportStatus status, String responseMessage) {
        Integer reportId = report.getId();
        User reporter = report.getReporter();
        String eventTitle = report.getEvent().getTitle();
        Runnable emailTask = () -> sendModerationEmail(reportId, reporter, eventTitle, status, responseMessage);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emailTask.run();
                }
            });
        } else {
            emailTask.run();
        }
    }

    private void sendModerationEmail(Integer reportId, User reporter, String eventTitle, ReportStatus status, String responseMessage) {
        try {
            if (status == ReportStatus.UNDER_REVIEW) {
                emailService.reportUnderReview(reporter, eventTitle);
            } else if (status == ReportStatus.RESOLVED) {
                emailService.reportResolved(reporter, eventTitle, responseMessage);
            } else if (status == ReportStatus.DISMISSED) {
                emailService.reportDismissed(reporter, eventTitle, responseMessage);
            }
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to send report moderation email for report {}", reportId, exception);
        }
    }

    private StatusReport status(Integer id) {
        return statusReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report status not found"));
    }
}
