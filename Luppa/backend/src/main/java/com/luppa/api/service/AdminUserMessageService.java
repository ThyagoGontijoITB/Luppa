package com.luppa.api.service;

import com.luppa.api.dto.AdminUserMessageRequest;
import com.luppa.api.entity.User;
import com.luppa.api.entity.UserRole;
import com.luppa.api.exception.ForbiddenOperationException;
import com.luppa.api.exception.InvalidRequestException;
import com.luppa.api.exception.ResourceNotFoundException;
import com.luppa.api.repository.UserRepository;
import com.luppa.api.security.AuthUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AdminUserMessageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminUserMessageService.class);

    private final UserRepository userRepository;
    private final AccountEmailService accountEmailService;
    private final AdminUserMessageRateLimiter rateLimiter;

    public AdminUserMessageService(
            UserRepository userRepository,
            AccountEmailService accountEmailService,
            AdminUserMessageRateLimiter rateLimiter
    ) {
        this.userRepository = userRepository;
        this.accountEmailService = accountEmailService;
        this.rateLimiter = rateLimiter;
    }

    public void send(Integer targetUserId, AdminUserMessageRequest request, AuthUser authUser) {
        UserRole actorRole = requireAdmin(authUser);
        String subject = normalize(request.subject());
        String message = normalize(request.message());
        validateTrimmed(subject, message);

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        authorizeTarget(actorRole, target);
        validateRecipient(target);

        if (!rateLimiter.tryAcquire(authUser.idUsuario(), target.getId(), subject, message)) {
            logOutcome(authUser.idUsuario(), target.getId(), "rate_limited");
            throw new InvalidRequestException("Aguarde antes de enviar novas mensagens administrativas.");
        }

        try {
            accountEmailService.adminUserMessage(target, subject, message);
            logOutcome(authUser.idUsuario(), target.getId(), "smtp_accepted");
        } catch (IllegalStateException exception) {
            logOutcome(authUser.idUsuario(), target.getId(), "smtp_failed");
            throw new InvalidRequestException("Não foi possível enviar a mensagem no momento. Tente novamente mais tarde.");
        }
    }

    private UserRole requireAdmin(AuthUser authUser) {
        if (authUser == null || authUser.user() == null || !authUser.user().getRole().getName().isAdminLike()) {
            throw new ForbiddenOperationException("Apenas administradores podem enviar mensagens administrativas.");
        }
        return authUser.user().getRole().getName();
    }

    private void authorizeTarget(UserRole actorRole, User target) {
        UserRole targetRole = target.getRole().getName();
        if (targetRole == UserRole.LUPPA_ADMIN) {
            throw new ForbiddenOperationException("Contas LUPPA_ADMIN não podem receber mensagens por este recurso administrativo.");
        }
        if (targetRole == UserRole.ADMIN && actorRole != UserRole.LUPPA_ADMIN) {
            throw new ForbiddenOperationException("Somente LUPPA_ADMIN pode enviar mensagens administrativas para administradores.");
        }
    }

    private void validateRecipient(User target) {
        if (target.getEmail() == null || target.getEmail().isBlank()) {
            throw new InvalidRequestException("Usuário sem e-mail válido para envio.");
        }
    }

    private void validateTrimmed(String subject, String message) {
        if (subject.length() < 5 || subject.length() > 150) {
            throw new InvalidRequestException("O assunto deve ter entre 5 e 150 caracteres.");
        }
        if (message.length() < 10 || message.length() > 5000) {
            throw new InvalidRequestException("A mensagem deve ter entre 10 e 5000 caracteres.");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private void logOutcome(Integer adminId, Integer targetUserId, String outcome) {
        LOGGER.info("admin_user_message adminId={} targetUserId={} timestamp={} outcome={}",
                adminId, targetUserId, Instant.now(), outcome);
    }
}
