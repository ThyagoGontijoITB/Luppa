package com.luppa.api.dto;

public record AdminUserMessageResponse(
        boolean accepted,
        String message
) {
}
