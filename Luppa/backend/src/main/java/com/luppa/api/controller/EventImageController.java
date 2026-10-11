package com.luppa.api.controller;

import com.luppa.api.dto.EventImageResponse;
import com.luppa.api.security.AuthUser;
import com.luppa.api.service.EventImageService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/events/{eventId}/image")
public class EventImageController {
    private final EventImageService eventImageService;

    public EventImageController(EventImageService eventImageService) {
        this.eventImageService = eventImageService;
    }

    @PostMapping
    public EventImageResponse replace(@PathVariable Integer eventId, @RequestPart("file") MultipartFile file, @AuthenticationPrincipal AuthUser authUser) {
        return eventImageService.replace(eventId, file, authUser);
    }

    @DeleteMapping
    public void delete(@PathVariable Integer eventId, @AuthenticationPrincipal AuthUser authUser) {
        eventImageService.delete(eventId, authUser);
    }
}
