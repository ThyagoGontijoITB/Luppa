package com.luppa.api.dto;

import com.luppa.api.entity.ReportStatus;

import java.time.LocalDateTime;

public record ReportResponse(
        Long id,
        Long eventId,
        String eventTitle,
        Long ratingId,
        ReportRatingTargetResponse rating,
        UserResponse reporter,
        ReportCategoryResponse category,
        String description,
        LocalDateTime reportedAt,
        ReportStatus status,
        String moderationResponse,
        LocalDateTime moderationRespondedAt
) {
}
