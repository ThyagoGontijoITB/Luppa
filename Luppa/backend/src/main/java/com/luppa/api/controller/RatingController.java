package com.luppa.api.controller;

import com.luppa.api.dto.RatingRequest;
import com.luppa.api.dto.RatingResponse;
import com.luppa.api.security.AuthUser;
import com.luppa.api.service.RatingService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RatingController {
    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @GetMapping("/events/{eventId}/ratings")
    public List<RatingResponse> byEvent(@PathVariable Integer eventId) {
        return ratingService.findByEvent(eventId);
    }

    @PostMapping("/events/{eventId}/ratings")
    public RatingResponse create(@PathVariable Integer eventId, @Valid @RequestBody RatingRequest request, @AuthenticationPrincipal AuthUser authUser) {
        return ratingService.upsert(eventId, request, authUser);
    }

    @PutMapping("/events/{eventId}/ratings/me")
    public RatingResponse updateMine(@PathVariable Integer eventId, @Valid @RequestBody RatingRequest request, @AuthenticationPrincipal AuthUser authUser) {
        return ratingService.upsert(eventId, request, authUser);
    }

    @DeleteMapping("/events/{eventId}/ratings/me")
    public void deleteMine(@PathVariable Integer eventId, @AuthenticationPrincipal AuthUser authUser) {
        ratingService.delete(eventId, authUser);
    }

    @GetMapping("/me/ratings")
    public List<RatingResponse> mine(@AuthenticationPrincipal AuthUser authUser) {
        return ratingService.findMine(authUser);
    }
}
