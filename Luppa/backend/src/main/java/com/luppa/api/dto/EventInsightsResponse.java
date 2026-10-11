package com.luppa.api.dto;

import java.util.List;

public record EventInsightsResponse(
        EventInsightsSummaryResponse summary,
        List<EventInsightsBreakdownResponse> byCategory,
        List<EventInsightsBreakdownResponse> byStatus,
        List<EventInsightsRankedEventResponse> mostSavedEvents,
        List<EventInsightsRankedEventResponse> bestRatedEvents
) {
}
