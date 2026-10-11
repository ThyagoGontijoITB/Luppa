package com.luppa.api.controller;

import com.luppa.api.dto.AdminUserMessageRequest;
import com.luppa.api.dto.AdminUserMessageResponse;
import com.luppa.api.security.AuthUser;
import com.luppa.api.service.AdminUserMessageService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserMessageController {
    private final AdminUserMessageService service;

    public AdminUserMessageController(AdminUserMessageService service) {
        this.service = service;
    }

    @PostMapping("/{idUsuario}/message")
    public AdminUserMessageResponse send(
            @PathVariable Integer idUsuario,
            @Valid @RequestBody AdminUserMessageRequest request,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        service.send(idUsuario, request, authUser);
        return new AdminUserMessageResponse(true, "Mensagem aceita para envio.");
    }
}
