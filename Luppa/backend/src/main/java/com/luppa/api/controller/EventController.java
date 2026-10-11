package com.luppa.api.controller;

import com.luppa.api.dto.EventCreateRequest;
import com.luppa.api.dto.EventResponse;
import com.luppa.api.dto.EventStatusUpdateRequest;
import com.luppa.api.dto.EventUpdateRequest;
import com.luppa.api.dto.PageResponse;
import com.luppa.api.entity.EventStatus;
import com.luppa.api.security.AuthUser;
import com.luppa.api.service.EventService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public PageResponse<EventResponse> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) EventStatus status,
            Pageable pageable
    ) {
        return PageResponse.from(eventService.search(search, category, status, pageable));
    }

    @GetMapping("/featured")
    public List<EventResponse> featured() {
        return eventService.findFeatured();
    }

    @GetMapping("/mine")
    public PageResponse<EventResponse> mine(@AuthenticationPrincipal AuthUser authUser, Pageable pageable) {
        return PageResponse.from(eventService.findMine(authUser, pageable));
    }

    @GetMapping("/{id}")
    public EventResponse get(@PathVariable Integer id) {
        return eventService.get(id);
    }

    @PostMapping
    public EventResponse create(@Valid @RequestBody EventCreateRequest request, @AuthenticationPrincipal AuthUser authUser) {
        return eventService.create(request, authUser);
    }

    @PutMapping("/{id}")
    public EventResponse update(@PathVariable Integer id, @Valid @RequestBody EventUpdateRequest request, @AuthenticationPrincipal AuthUser authUser) {
        return eventService.update(id, request, authUser);
    }

    @PutMapping("/{id}/status")
    public EventResponse updateStatus(@PathVariable Integer id, @Valid @RequestBody EventStatusUpdateRequest request, @AuthenticationPrincipal AuthUser authUser) {
        return eventService.updateStatus(id, request, authUser);
    }

    @PutMapping("/{id}/featured")
    public EventResponse updateFeatured(@PathVariable Integer id, @AuthenticationPrincipal AuthUser authUser) {
        return eventService.updateFeatured(id, authUser);
    }

    @DeleteMapping("/featured")
    public void clearFeatured(@AuthenticationPrincipal AuthUser authUser) {
        eventService.clearFeatured(authUser);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id, @AuthenticationPrincipal AuthUser authUser) {
        eventService.delete(id, authUser);
    }
}
