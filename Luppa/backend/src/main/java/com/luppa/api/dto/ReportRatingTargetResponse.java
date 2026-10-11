package com.luppa.api.dto;

import java.time.LocalDateTime;

public record ReportRatingTargetResponse(
        Long id,
        PublicUserResponse author,
        int score,
        String description,
        LocalDateTime ratedAt
) {
}
