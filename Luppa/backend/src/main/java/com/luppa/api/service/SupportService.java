package com.luppa.api.service;

import com.luppa.api.dto.SupportContactRequest;
import com.luppa.api.dto.SupportContactResponse;
import com.luppa.api.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class SupportService {
    private final AccountEmailService emailService;

    public SupportService(AccountEmailService emailService) {
        this.emailService = emailService;
    }

    @Transactional
    public SupportContactResponse create(SupportContactRequest request, AuthUser authUser) {
        Integer userId = authUser == null ? null : authUser.idUsuario();
        String timestamp = Instant.now().toString();
        emailService.supportConfirmation(request.name(), request.email(), request.category(), request.subject());
        emailService.internalSupport(request.name(), request.email(), userId, request.category(), request.subject(), request.message(), timestamp);
        return new SupportContactResponse(null, "Solicitação enviada para a equipe Luppa.");
    }
}
