package com.luppa.api.service.image;

public record StoredImage(
        String reference,
        String url,
        String mimeType,
        long fileSize,
        Integer width,
        Integer height
) {
}
