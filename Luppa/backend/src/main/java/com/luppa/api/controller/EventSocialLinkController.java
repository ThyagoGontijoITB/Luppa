package com.luppa.api.controller;

import com.luppa.api.dto.EventSocialLinkResponse;
import com.luppa.api.service.EventSocialLinkService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events/{eventId}/social-links")
public class EventSocialLinkController {
    private final EventSocialLinkService eventSocialLinkService;

    public EventSocialLinkController(EventSocialLinkService eventSocialLinkService) {
        this.eventSocialLinkService = eventSocialLinkService;
    }

    @GetMapping
    public List<EventSocialLinkResponse> list(@PathVariable Integer eventId) {
        return eventSocialLinkService.findByEvent(eventId);
    }
}
