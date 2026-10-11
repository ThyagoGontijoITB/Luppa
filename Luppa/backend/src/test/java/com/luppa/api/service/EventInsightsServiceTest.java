package com.luppa.api.service;

import com.luppa.api.dto.EventInsightsResponse;
import com.luppa.api.entity.Role;
import com.luppa.api.entity.User;
import com.luppa.api.entity.UserRole;
import com.luppa.api.exception.ForbiddenOperationException;
import com.luppa.api.repository.EventInsightsBreakdownView;
import com.luppa.api.repository.EventInsightsCountView;
import com.luppa.api.repository.EventInsightsRankedEventView;
import com.luppa.api.repository.EventRepository;
import com.luppa.api.repository.RatingRepository;
import com.luppa.api.repository.SavedEventRepository;
import com.luppa.api.security.AuthUser;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventInsightsServiceTest {
    private final EventRepository eventRepository = mock(EventRepository.class);
    private final SavedEventRepository savedEventRepository = mock(SavedEventRepository.class);
    private final RatingRepository ratingRepository = mock(RatingRepository.class);
    private final EventInsightsService service = new EventInsightsService(eventRepository, savedEventRepository, ratingRepository);

    @Test
    void creatorInsightsUseAuthenticatedCreatorOnly() {
        AuthUser authUser = authUser(42, UserRole.CREATOR);
        when(eventRepository.countInsightsByCreator(42)).thenReturn(counts(3L, 1L, 2L, 0L, 0L));
        when(savedEventRepository.countByEventCreatorId(42)).thenReturn(7L);
        when(ratingRepository.countByEventCreatorId(42)).thenReturn(4L);
        when(ratingRepository.averageScoreByEventCreatorId(42)).thenReturn(4.25);
        when(eventRepository.countByCategoryForCreator(42)).thenReturn(List.of(breakdown("Música", "Música", 2L)));
        when(eventRepository.countByStatusForCreator(42)).thenReturn(List.of(breakdown("0", "Publicado", 1L), breakdown("1", "Rascunho", 2L)));
        when(savedEventRepository.findMostSavedByCreator(eq(42), any(Pageable.class))).thenReturn(List.of(ranked(10, "Show", 0, 7L, 0L, null)));
        when(ratingRepository.findBestRatedByCreator(eq(42), any(Pageable.class))).thenReturn(List.of(ranked(11, "Feira", 0, 0L, 4L, 4.25)));

        EventInsightsResponse response = service.creatorInsights(authUser);

        assertThat(response.summary().totalEvents()).isEqualTo(3);
        assertThat(response.summary().publishedEvents()).isEqualTo(1);
        assertThat(response.summary().draftEvents()).isEqualTo(2);
        assertThat(response.summary().totalSaves()).isEqualTo(7);
        assertThat(response.summary().totalReviews()).isEqualTo(4);
        assertThat(response.summary().averageRating()).isEqualTo(4.3);
        assertThat(response.byCategory()).extracting("label").containsExactly("Música");
        assertThat(response.byStatus()).extracting("key").containsExactly("PUBLISHED", "DRAFT");
        assertThat(response.mostSavedEvents().getFirst().eventId()).isEqualTo(10L);
        assertThat(response.bestRatedEvents().getFirst().averageRating()).isEqualTo(4.3);
        verify(eventRepository).countInsightsByCreator(42);
        verify(eventRepository, never()).countPlatformInsights();
    }

    @Test
    void standardUserCannotAccessCreatorInsights() {
        assertThatThrownBy(() -> service.creatorInsights(authUser(8, UserRole.USER)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void adminAndLuppaAdminCanAccessPlatformInsights() {
        when(eventRepository.countPlatformInsights()).thenReturn(counts(0L, 0L, 0L, 0L, 0L));
        when(eventRepository.countByCategoryForPlatform()).thenReturn(List.of());
        when(eventRepository.countByStatusForPlatform()).thenReturn(List.of());
        when(savedEventRepository.findMostSavedForPlatform(any(Pageable.class))).thenReturn(List.of());
        when(ratingRepository.findBestRatedForPlatform(any(Pageable.class))).thenReturn(List.of());

        assertThat(service.platformInsights(authUser(1, UserRole.ADMIN)).summary().totalEvents()).isZero();
        assertThat(service.platformInsights(authUser(2, UserRole.LUPPA_ADMIN)).summary().totalEvents()).isZero();
    }

    @Test
    void creatorCannotAccessPlatformInsights() {
        assertThatThrownBy(() -> service.platformInsights(authUser(9, UserRole.CREATOR)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void emptyDatasetDoesNotFabricateAnalytics() {
        when(eventRepository.countInsightsByCreator(42)).thenReturn(counts(null, null, null, null, null));
        when(ratingRepository.averageScoreByEventCreatorId(42)).thenReturn(null);
        when(eventRepository.countByCategoryForCreator(42)).thenReturn(List.of());
        when(eventRepository.countByStatusForCreator(42)).thenReturn(List.of());
        when(savedEventRepository.findMostSavedByCreator(eq(42), any(Pageable.class))).thenReturn(List.of());
        when(ratingRepository.findBestRatedByCreator(eq(42), any(Pageable.class))).thenReturn(List.of());

        EventInsightsResponse response = service.creatorInsights(authUser(42, UserRole.CREATOR));

        assertThat(response.summary().totalEvents()).isZero();
        assertThat(response.summary().totalSaves()).isZero();
        assertThat(response.summary().totalReviews()).isZero();
        assertThat(response.summary().averageRating()).isNull();
        assertThat(response.mostSavedEvents()).isEmpty();
        assertThat(response.bestRatedEvents()).isEmpty();
    }

    private AuthUser authUser(Integer id, UserRole roleName) {
        User user = new User();
        setId(user, id);
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setDateOfBirth(LocalDate.of(1990, 1, 1));
        user.setPhone("11999999999");
        user.setPasswordHash("hash");
        Role role = new Role();
        role.setName(roleName);
        user.setRole(role);
        user.setEmailVerified(true);
        return new AuthUser(user);
    }

    private EventInsightsCountView counts(Long total, Long published, Long draft, Long review, Long cancelled) {
        return new EventInsightsCountView() {
            @Override public Long getTotalEvents() { return total; }
            @Override public Long getPublishedEvents() { return published; }
            @Override public Long getDraftEvents() { return draft; }
            @Override public Long getReviewEvents() { return review; }
            @Override public Long getCancelledEvents() { return cancelled; }
        };
    }

    private EventInsightsBreakdownView breakdown(String key, String label, Long count) {
        return new EventInsightsBreakdownView() {
            @Override public String getKey() { return key; }
            @Override public String getLabel() { return label; }
            @Override public Long getCount() { return count; }
        };
    }

    private EventInsightsRankedEventView ranked(Integer eventId, String title, Integer statusId, Long saves, Long reviews, Double averageRating) {
        return new EventInsightsRankedEventView() {
            @Override public Integer getEventId() { return eventId; }
            @Override public String getTitle() { return title; }
            @Override public String getCategoryName() { return "Categoria"; }
            @Override public Integer getStatusId() { return statusId; }
            @Override public Long getSaveCount() { return saves; }
            @Override public Long getReviewCount() { return reviews; }
            @Override public Double getAverageRating() { return averageRating; }
        };
    }

    private void setId(User user, Integer id) {
        try {
            Field field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
