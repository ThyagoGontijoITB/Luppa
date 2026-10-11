package com.luppa.api.dto;

import com.luppa.api.entity.UserRole;
import com.luppa.api.entity.UserStatus;

import java.time.Instant;
import java.time.LocalDate;

public record UserResponse(
        Long id,
        String name,
        String email,
        LocalDate dateOfBirth,
        String phone,
        String profileImageUrl,
        String profileImagePath,
        UserRole role,
        UserStatus status,
        boolean emailVerified,
        Instant createdAt,
        Instant updatedAt
) {
}
