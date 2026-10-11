package com.luppa.api.controller;

import com.luppa.api.dto.SavedEventResponse;
import com.luppa.api.dto.SavedStatusResponse;
import com.luppa.api.security.AuthUser;
import com.luppa.api.service.SavedEventService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SavedEventController {
    private final SavedEventService savedEventService;

    public SavedEventController(SavedEventService savedEventService) {
        this.savedEventService = savedEventService;
    }

    @PostMapping("/events/{eventId}/save")
    public SavedEventResponse save(@PathVariable Integer eventId, @AuthenticationPrincipal AuthUser authUser) {
        return savedEventService.save(eventId, authUser);
    }

    @DeleteMapping("/events/{eventId}/save")
    public void unsave(@PathVariable Integer eventId, @AuthenticationPrincipal AuthUser authUser) {
        savedEventService.unsave(eventId, authUser);
    }

    @GetMapping("/events/{eventId}/save")
    public SavedStatusResponse status(@PathVariable Integer eventId, @AuthenticationPrincipal AuthUser authUser) {
        return savedEventService.status(eventId, authUser);
    }

    @GetMapping("/me/saved-events")
    public List<SavedEventResponse> mine(@AuthenticationPrincipal AuthUser authUser) {
        return savedEventService.findMine(authUser);
    }
}
