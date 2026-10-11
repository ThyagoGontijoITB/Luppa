package com.luppa.api.dto;

public record PublicUserResponse(
        Long id,
        String name,
        String profileImageUrl
) {
}
