package com.luppa.api.dto;

public record EventInsightsBreakdownResponse(
        String key,
        String label,
        long count
) {
}
