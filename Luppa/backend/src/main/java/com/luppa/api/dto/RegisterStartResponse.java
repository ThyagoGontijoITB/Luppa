package com.luppa.api.dto;

public record RegisterStartResponse(
        String email,
        int codeExpiresInSeconds,
        int resendCooldownSeconds
) {
}
