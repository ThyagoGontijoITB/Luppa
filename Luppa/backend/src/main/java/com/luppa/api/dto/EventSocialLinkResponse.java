package com.luppa.api.dto;

public record EventSocialLinkResponse(
        Long socialProfileId,
        String platform,
        String url
) {
}
