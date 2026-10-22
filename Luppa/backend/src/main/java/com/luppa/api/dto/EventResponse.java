package com.luppa.api.dto;

import com.luppa.api.entity.EventStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public record EventResponse(
        Long id,
        String title,
        String description,
        CategoryResponse category,
        String location,
        EventAddressResponse address,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        UserResponse creator,
        EventStatus status,
        boolean destaquePrincipal,
        boolean featured,
        EventImageResponse coverImage,
        Instant createdAt,
        Instant updatedAt,
        long interestCount
) {
}
