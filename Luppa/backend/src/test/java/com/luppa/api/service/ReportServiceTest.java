package com.luppa.api.service;

import com.luppa.api.dto.ReportCreateRequest;
import com.luppa.api.dto.ReportStatusUpdateRequest;
import com.luppa.api.entity.Event;
import com.luppa.api.entity.Rating;
import com.luppa.api.entity.Report;
import com.luppa.api.entity.ReportCategory;
import com.luppa.api.entity.ReportStatus;
import com.luppa.api.entity.Role;
import com.luppa.api.entity.StatusReport;
import com.luppa.api.entity.User;
import com.luppa.api.entity.UserRole;
import com.luppa.api.exception.InvalidRequestException;
import com.luppa.api.repository.EventRepository;
import com.luppa.api.repository.RatingRepository;
import com.luppa.api.repository.ReportCategoryRepository;
import com.luppa.api.repository.ReportRepository;
import com.luppa.api.repository.StatusReportRepository;
import com.luppa.api.security.AuthUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportServiceTest {
    private final ReportRepository reportRepository = mock(ReportRepository.class);
    private final ReportCategoryRepository reportCategoryRepository = mock(ReportCategoryRepository.class);
    private final EventRepository eventRepository = mock(EventRepository.class);
    private final RatingRepository ratingRepository = mock(RatingRepository.class);
    private final StatusReportRepository statusReportRepository = mock(StatusReportRepository.class);
    private final AccountEmailService emailService = mock(AccountEmailService.class);
    private final ReportService service = new ReportService(
            reportRepository,
            reportCategoryRepository,
            eventRepository,
            ratingRepository,
            statusReportRepository,
            emailService
    );

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void createAssociatesReportWithRatingWhenRatingBelongsToEvent() {
        User reporter = user(10, UserRole.USER);
        Event event = event(20, "Evento");
        Rating rating = rating(30, event, user(11, UserRole.USER));
        when(eventRepository.findById(20)).thenReturn(Optional.of(event));
        when(reportCategoryRepository.findById(5)).thenReturn(Optional.of(category(5)));
        when(statusReportRepository.findById(0)).thenReturn(Optional.of(status(0)));
        when(ratingRepository.findById(30)).thenReturn(Optional.of(rating));
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> {
            Report report = invocation.getArgument(0);
            set(report, "id", 40);
            return report;
        });

        service.create(20, new ReportCreateRequest(5, "Conteúdo impróprio", 30), new AuthUser(reporter));

        ArgumentCaptor<Report> reportCaptor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(reportCaptor.capture());
        assertThat(reportCaptor.getValue().getRating()).isSameAs(rating);
        assertThat(reportCaptor.getValue().getReporter()).isSameAs(reporter);
    }

    @Test
    void createRejectsRatingFromAnotherEvent() {
        Event event = event(20, "Evento");
        Rating rating = rating(30, event(21, "Outro evento"), user(11, UserRole.USER));
        when(eventRepository.findById(20)).thenReturn(Optional.of(event));
        when(reportCategoryRepository.findById(5)).thenReturn(Optional.of(category(5)));
        when(statusReportRepository.findById(0)).thenReturn(Optional.of(status(0)));
        when(ratingRepository.findById(30)).thenReturn(Optional.of(rating));

        assertThatThrownBy(() -> service.create(20, new ReportCreateRequest(5, "Conteúdo impróprio", 30), new AuthUser(user(10, UserRole.USER))))
                .isInstanceOf(InvalidRequestException.class);
        verify(reportRepository, never()).save(any());
    }

    @Test
    void finalModerationStatusesRequireResponseMessage() {
        Report report = report(40, ReportStatus.PENDING);
        when(reportRepository.findById(40)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> service.updateStatus(40, new ReportStatusUpdateRequest(ReportStatus.RESOLVED, " "), new AuthUser(user(99, UserRole.ADMIN))))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void resolvedStatusPersistsModerationResponse() {
        Report report = report(40, ReportStatus.PENDING);
        when(reportRepository.findById(40)).thenReturn(Optional.of(report));
        when(statusReportRepository.findById(2)).thenReturn(Optional.of(status(2)));

        service.updateStatus(40, new ReportStatusUpdateRequest(ReportStatus.RESOLVED, "Analisamos e tomamos providências."), new AuthUser(user(99, UserRole.ADMIN)));

        assertThat(report.getModerationResponse()).isEqualTo("Analisamos e tomamos providências.");
        assertThat(report.getModerationRespondedAt()).isNotNull();
    }

    @Test
    void moderationEmailIsSentOnlyAfterCommitAndFailureDoesNotBubble() {
        Report report = report(40, ReportStatus.PENDING);
        when(reportRepository.findById(40)).thenReturn(Optional.of(report));
        when(statusReportRepository.findById(2)).thenReturn(Optional.of(status(2)));
        org.mockito.Mockito.doThrow(new IllegalStateException("smtp down"))
                .when(emailService).reportResolved(report.getReporter(), report.getEvent().getTitle(), "Resposta");

        TransactionSynchronizationManager.initSynchronization();
        service.updateStatus(40, new ReportStatusUpdateRequest(ReportStatus.RESOLVED, "Resposta"), new AuthUser(user(99, UserRole.ADMIN)));

        verify(emailService, never()).reportResolved(any(), any(), any());
        assertThatCode(() -> TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit))
                .doesNotThrowAnyException();
        verify(emailService).reportResolved(report.getReporter(), report.getEvent().getTitle(), "Resposta");
    }

    private Report report(Integer id, ReportStatus status) {
        Report report = new Report();
        set(report, "id", id);
        report.setEvent(event(20, "Evento"));
        report.setReporter(user(10, UserRole.USER));
        report.setCategory(category(5));
        report.setStatus(status(StatusReport.idFor(status)));
        report.setDescription("Denúncia");
        return report;
    }

    private Rating rating(Integer id, Event event, User user) {
        Rating rating = new Rating();
        set(rating, "id", id);
        rating.setEvent(event);
        rating.setUser(user);
        rating.setScore((byte) 5);
        rating.setDescription("Comentário");
        return rating;
    }

    private Event event(Integer id, String title) {
        Event event = new Event();
        set(event, "id", id);
        event.setTitle(title);
        return event;
    }

    private ReportCategory category(Integer id) {
        ReportCategory category = new ReportCategory();
        set(category, "id", id);
        set(category, "category", "Conteúdo impróprio");
        return category;
    }

    private StatusReport status(Integer id) {
        StatusReport status = new StatusReport();
        set(status, "id", id);
        return status;
    }

    private User user(Integer id, UserRole roleName) {
        Role role = new Role();
        role.setName(roleName);
        User user = new User();
        set(user, "id", id);
        user.setName(roleName == UserRole.ADMIN ? "Admin Luppa" : "User Test");
        user.setEmail(roleName == UserRole.ADMIN ? "admin@luppa.com" : "user@luppa.com");
        user.setPhone("11999999999");
        user.setDateOfBirth(LocalDate.of(1990, 1, 1));
        user.setPasswordHash("hash");
        user.setRole(role);
        user.setEmailVerified(true);
        return user;
    }

    private void set(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
