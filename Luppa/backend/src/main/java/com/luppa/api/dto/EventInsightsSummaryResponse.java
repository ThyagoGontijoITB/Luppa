package com.luppa.api.dto;

public record EventInsightsSummaryResponse(
        long totalEvents,
        long publishedEvents,
        long draftEvents,
        long reviewEvents,
        long cancelledEvents,
        long totalSaves,
        long totalReviews,
        Double averageRating
) {
}
