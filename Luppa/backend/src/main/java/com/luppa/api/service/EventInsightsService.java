package com.luppa.api.service;

import com.luppa.api.dto.EventInsightsBreakdownResponse;
import com.luppa.api.dto.EventInsightsRankedEventResponse;
import com.luppa.api.dto.EventInsightsResponse;
import com.luppa.api.dto.EventInsightsSummaryResponse;
import com.luppa.api.entity.EventStatus;
import com.luppa.api.entity.UserRole;
import com.luppa.api.exception.ForbiddenOperationException;
import com.luppa.api.repository.EventInsightsBreakdownView;
import com.luppa.api.repository.EventInsightsCountView;
import com.luppa.api.repository.EventInsightsRankedEventView;
import com.luppa.api.repository.EventRepository;
import com.luppa.api.repository.RatingRepository;
import com.luppa.api.repository.SavedEventRepository;
import com.luppa.api.security.AuthUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EventInsightsService {
    private static final int RANKING_LIMIT = 5;

    private final EventRepository eventRepository;
    private final SavedEventRepository savedEventRepository;
    private final RatingRepository ratingRepository;

    public EventInsightsService(
            EventRepository eventRepository,
            SavedEventRepository savedEventRepository,
            RatingRepository ratingRepository
    ) {
        this.eventRepository = eventRepository;
        this.savedEventRepository = savedEventRepository;
        this.ratingRepository = ratingRepository;
    }

    @Transactional(readOnly = true)
    public EventInsightsResponse creatorInsights(AuthUser authUser) {
        requireCreatorOrAdmin(authUser);
        Integer creatorId = authUser.idUsuario();
        return buildResponse(
                eventRepository.countInsightsByCreator(creatorId),
                savedEventRepository.countByEventCreatorId(creatorId),
                ratingRepository.countByEventCreatorId(creatorId),
                ratingRepository.averageScoreByEventCreatorId(creatorId),
                eventRepository.countByCategoryForCreator(creatorId),
                eventRepository.countByStatusForCreator(creatorId),
                savedEventRepository.findMostSavedByCreator(creatorId, PageRequest.of(0, RANKING_LIMIT)),
                ratingRepository.findBestRatedByCreator(creatorId, PageRequest.of(0, RANKING_LIMIT))
        );
    }

    @Transactional(readOnly = true)
    public EventInsightsResponse platformInsights(AuthUser authUser) {
        requireAdmin(authUser);
        return buildResponse(
                eventRepository.countPlatformInsights(),
                savedEventRepository.countAllSavedEvents(),
                ratingRepository.count(),
                ratingRepository.averageScoreForPlatform(),
                eventRepository.countByCategoryForPlatform(),
                eventRepository.countByStatusForPlatform(),
                savedEventRepository.findMostSavedForPlatform(PageRequest.of(0, RANKING_LIMIT)),
                ratingRepository.findBestRatedForPlatform(PageRequest.of(0, RANKING_LIMIT))
        );
    }

    private EventInsightsResponse buildResponse(
            EventInsightsCountView counts,
            long totalSaves,
            long totalReviews,
            Double averageRating,
            List<EventInsightsBreakdownView> byCategory,
            List<EventInsightsBreakdownView> byStatus,
            List<EventInsightsRankedEventView> mostSaved,
            List<EventInsightsRankedEventView> bestRated
    ) {
        return new EventInsightsResponse(
                new EventInsightsSummaryResponse(
                        value(counts.getTotalEvents()),
                        value(counts.getPublishedEvents()),
                        value(counts.getDraftEvents()),
                        value(counts.getReviewEvents()),
                        value(counts.getCancelledEvents()),
                        totalSaves,
                        totalReviews,
                        roundOne(averageRating)
                ),
                byCategory.stream().map(this::categoryBreakdown).toList(),
                byStatus.stream().map(this::statusBreakdown).toList(),
                mostSaved.stream().map(this::rankedEvent).toList(),
                bestRated.stream().map(this::rankedEvent).toList()
        );
    }

    private EventInsightsBreakdownResponse categoryBreakdown(EventInsightsBreakdownView view) {
        return new EventInsightsBreakdownResponse(view.getKey(), view.getLabel(), value(view.getCount()));
    }

    private EventInsightsBreakdownResponse statusBreakdown(EventInsightsBreakdownView view) {
        EventStatus status = statusFromId(Integer.valueOf(view.getKey()));
        return new EventInsightsBreakdownResponse(status.name(), labelFor(status), value(view.getCount()));
    }

    private EventInsightsRankedEventResponse rankedEvent(EventInsightsRankedEventView view) {
        return new EventInsightsRankedEventResponse(
                view.getEventId().longValue(),
                view.getTitle(),
                view.getCategoryName(),
                statusFromId(view.getStatusId()),
                value(view.getSaveCount()),
                value(view.getReviewCount()),
                roundOne(view.getAverageRating())
        );
    }

    private EventStatus statusFromId(Integer statusId) {
        if (statusId == 0) return EventStatus.PUBLISHED;
        if (statusId == 1) return EventStatus.DRAFT;
        if (statusId == 2) return EventStatus.CANCELLED;
        if (statusId == 3) return EventStatus.REVIEW;
        throw new IllegalStateException("Unknown statusEvento idStatus: " + statusId);
    }

    private String labelFor(EventStatus status) {
        if (status == EventStatus.PUBLISHED) return "Publicado";
        if (status == EventStatus.DRAFT) return "Rascunho";
        if (status == EventStatus.REVIEW) return "Em análise";
        if (status == EventStatus.CANCELLED) return "Cancelado";
        throw new IllegalStateException("Unknown EventStatus: " + status);
    }

    private void requireCreatorOrAdmin(AuthUser authUser) {
        UserRole role = authUser.user().getRole().getName();
        if (role != UserRole.CREATOR && !role.isAdminLike()) {
            throw new ForbiddenOperationException("Only creators or admins can access creator event insights");
        }
    }

    private void requireAdmin(AuthUser authUser) {
        if (!authUser.user().getRole().getName().isAdminLike()) {
            throw new ForbiddenOperationException("Only admins can access platform event insights");
        }
    }

    private long value(Long value) {
        return value == null ? 0 : value;
    }

    private Double roundOne(Double value) {
        if (value == null) {
            return null;
        }
        return Math.round(value * 10.0) / 10.0;
    }
}
