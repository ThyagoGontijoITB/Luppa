package com.luppa.api.dto;

import java.time.Instant;

public record ApiErrorResponse(
        int status,
        String message,
        Instant timestamp,
        String path
) {
}
