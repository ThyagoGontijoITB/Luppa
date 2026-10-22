package com.luppa.api.mapper;

import com.luppa.api.dto.EventResponse;
import com.luppa.api.dto.EventAddressResponse;
import com.luppa.api.entity.Address;
import com.luppa.api.entity.Event;

public final class EventMapper {
    private EventMapper() {
    }

    public static EventResponse toResponse(Event event, boolean featured, long interestCount) {
        return new EventResponse(
                event.getId().longValue(),
                event.getTitle(),
                event.getDescription(),
                CategoryMapper.toResponse(event.getCategory()),
                event.getLocation(),
                toAddressResponse(event.getAddress()),
                event.getLatitude(),
                event.getLongitude(),
                event.getStartDateTime(),
                event.getEndDateTime(),
                UserMapper.toResponse(event.getCreator()),
                event.getStatus(),
                event.isDestaquePrincipal(),
                featured,
                EventImageMapper.toResponse(event.getCoverImage()),
                event.getCreatedAt(),
                event.getUpdatedAt(),
                interestCount
        );
    }

    private static EventAddressResponse toAddressResponse(Address address) {
        if (address == null) {
            return null;
        }
        return new EventAddressResponse(
                address.getZipCode(),
                address.getStreet(),
                address.getNumber(),
                address.getCity(),
                address.getState(),
                address.getCountry()
        );
    }
}
