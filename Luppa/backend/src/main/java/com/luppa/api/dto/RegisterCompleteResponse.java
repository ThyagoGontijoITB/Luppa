package com.luppa.api.dto;

public record RegisterCompleteResponse(
        String email,
        boolean completed
) {
}
