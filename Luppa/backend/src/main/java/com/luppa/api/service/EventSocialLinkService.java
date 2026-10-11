package com.luppa.api.service;

import com.luppa.api.dto.EventSocialLinkResponse;
import com.luppa.api.mapper.EventSocialLinkMapper;
import com.luppa.api.repository.EventSocialLinkRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventSocialLinkService {
    private final EventSocialLinkRepository eventSocialLinkRepository;

    public EventSocialLinkService(EventSocialLinkRepository eventSocialLinkRepository) {
        this.eventSocialLinkRepository = eventSocialLinkRepository;
    }

    public List<EventSocialLinkResponse> findByEvent(Integer eventId) {
        return eventSocialLinkRepository.findByIdEventId(eventId).stream()
                .map(EventSocialLinkMapper::toResponse)
                .toList();
    }
}
