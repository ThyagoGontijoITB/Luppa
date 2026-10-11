package com.luppa.api.dto;

import java.time.Instant;

public record EventImageResponse(
        Long id,
        String fileName,
        String storageKey,
        String url,
        String mimeType,
        long fileSize,
        Integer width,
        Integer height,
        Instant createdAt
) {
}
