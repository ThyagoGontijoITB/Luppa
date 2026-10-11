package com.luppa.api.controller;

import com.luppa.api.dto.EventInsightsResponse;
import com.luppa.api.security.AuthUser;
import com.luppa.api.service.EventInsightsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/insights/events")
public class EventInsightsController {
    private final EventInsightsService eventInsightsService;

    public EventInsightsController(EventInsightsService eventInsightsService) {
        this.eventInsightsService = eventInsightsService;
    }

    @GetMapping("/creator")
    public EventInsightsResponse creator(@AuthenticationPrincipal AuthUser authUser) {
        return eventInsightsService.creatorInsights(authUser);
    }

    @GetMapping("/platform")
    public EventInsightsResponse platform(@AuthenticationPrincipal AuthUser authUser) {
        return eventInsightsService.platformInsights(authUser);
    }
}
