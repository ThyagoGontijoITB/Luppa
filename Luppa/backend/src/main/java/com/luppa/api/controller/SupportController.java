package com.luppa.api.controller;

import com.luppa.api.dto.SupportContactRequest;
import com.luppa.api.dto.SupportContactResponse;
import com.luppa.api.security.AuthUser;
import com.luppa.api.security.PublicEndpointRateLimiter;
import com.luppa.api.service.SupportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/support")
public class SupportController {
    private final SupportService supportService;
    private final PublicEndpointRateLimiter rateLimiter;

    public SupportController(SupportService supportService, PublicEndpointRateLimiter rateLimiter) {
        this.supportService = supportService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/contact")
    public SupportContactResponse contact(@Valid @RequestBody SupportContactRequest request, @AuthenticationPrincipal AuthUser authUser, HttpServletRequest servletRequest) {
        rateLimiter.checkPublicSupport(servletRequest, request.email());
        return supportService.create(request, authUser);
    }
}
