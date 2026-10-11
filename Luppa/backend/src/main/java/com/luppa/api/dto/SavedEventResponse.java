package com.luppa.api.dto;

import java.time.LocalDateTime;

public record SavedEventResponse(
        Long id,
        EventResponse event,
        LocalDateTime savedAt
) {
}
