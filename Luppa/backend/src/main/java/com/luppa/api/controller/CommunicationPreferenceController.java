package com.luppa.api.controller;

import com.luppa.api.dto.CommunicationPreferencesResponse;
import com.luppa.api.dto.CommunicationPreferencesUpdateRequest;
import com.luppa.api.security.AuthUser;
import com.luppa.api.service.CommunicationPreferenceService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/communication-preferences")
public class CommunicationPreferenceController {
    private final CommunicationPreferenceService service;

    public CommunicationPreferenceController(CommunicationPreferenceService service) {
        this.service = service;
    }

    @GetMapping
    public CommunicationPreferencesResponse get(@AuthenticationPrincipal AuthUser authUser) {
        return service.get(authUser.idUsuario());
    }

    @PutMapping
    public CommunicationPreferencesResponse update(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody CommunicationPreferencesUpdateRequest request
    ) {
        return service.update(authUser.idUsuario(), request);
    }
}
