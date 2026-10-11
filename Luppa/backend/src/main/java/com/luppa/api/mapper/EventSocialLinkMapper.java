package com.luppa.api.mapper;

import com.luppa.api.dto.EventSocialLinkResponse;
import com.luppa.api.entity.EventSocialLink;

public final class EventSocialLinkMapper {
    private EventSocialLinkMapper() {
    }

    public static EventSocialLinkResponse toResponse(EventSocialLink link) {
        return new EventSocialLinkResponse(
                link.getSocialProfile().getId().longValue(),
                link.getSocialProfile().getPlatform(),
                link.getUrl()
        );
    }
}
