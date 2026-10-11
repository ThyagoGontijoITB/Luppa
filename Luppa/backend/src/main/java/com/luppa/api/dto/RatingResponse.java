package com.luppa.api.dto;

import java.time.LocalDateTime;

public record RatingResponse(
        Long id,
        Long eventId,
        PublicUserResponse user,
        int score,
        String description,
        LocalDateTime ratedAt
) {
}
