package com.luppa.api.dto;

public record LoginResponse(
        String token,
        UserResponse user,
        boolean requiresEmailVerification
) {
}
