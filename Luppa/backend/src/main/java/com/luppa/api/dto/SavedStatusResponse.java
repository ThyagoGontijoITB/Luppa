package com.luppa.api.dto;

public record SavedStatusResponse(
        Long eventId,
        boolean saved
) {
}
