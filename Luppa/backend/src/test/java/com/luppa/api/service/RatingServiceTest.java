package com.luppa.api.service;

import com.luppa.api.config.StorageProperties;
import com.luppa.api.dto.RatingResponse;
import com.luppa.api.mapper.UserMapper;
import com.luppa.api.repository.EventRatingView;
import com.luppa.api.repository.EventRepository;
import com.luppa.api.repository.RatingRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RatingServiceTest {
    private final RatingRepository ratingRepository = mock(RatingRepository.class);
    private final EventRepository eventRepository = mock(EventRepository.class);
    private final RatingService service = new RatingService(ratingRepository, eventRepository);

    RatingServiceTest() {
        new UserMapper(new StorageProperties("uploads/events", "http://localhost:8081/uploads/events", "uploads/profile", "http://localhost:8081/uploads/profile"));
    }

    @Test
    void publicEventRatingsExposeOnlyPublicAuthorData() {
        when(ratingRepository.findPublicByEventIdOrderByRatedAtDesc(20)).thenReturn(List.of(new EventRatingView() {
            @Override public Integer getId() { return 30; }
            @Override public Integer getEventId() { return 20; }
            @Override public Integer getUserId() { return 10; }
            @Override public String getFirstName() { return "Ana"; }
            @Override public String getLastName() { return "Silva"; }
            @Override public String getProfileImagePath() { return "ana.png"; }
            @Override public Byte getScore() { return 5; }
            @Override public String getDescription() { return "Ótimo evento"; }
            @Override public LocalDateTime getRatedAt() { return LocalDateTime.of(2026, 10, 5, 10, 0); }
        }));

        List<RatingResponse> responses = service.findByEvent(20);

        assertThat(responses).hasSize(1);
        RatingResponse response = responses.getFirst();
        assertThat(response.user().name()).isEqualTo("Ana Silva");
        assertThat(response.user().profileImageUrl()).isEqualTo("http://localhost:8081/uploads/profile/ana.png");
        assertThat(response.user().getClass().getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .containsExactly("id", "name", "profileImageUrl");
    }
}
