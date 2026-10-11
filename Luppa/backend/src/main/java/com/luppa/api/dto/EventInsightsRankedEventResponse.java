package com.luppa.api.dto;

import com.luppa.api.entity.EventStatus;

public record EventInsightsRankedEventResponse(
        Long eventId,
        String title,
        String categoryName,
        EventStatus status,
        long saveCount,
        long reviewCount,
        Double averageRating
) {
}
