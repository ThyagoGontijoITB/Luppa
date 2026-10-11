package com.luppa.api.service;

import com.luppa.api.dto.RegisterCompleteResponse;
import com.luppa.api.dto.RegisterRequest;
import com.luppa.api.dto.RegisterResendRequest;
import com.luppa.api.dto.RegisterStartResponse;
import com.luppa.api.dto.RegisterVerifyRequest;
import com.luppa.api.entity.PendingRegistration;
import com.luppa.api.entity.Role;
import com.luppa.api.entity.User;
import com.luppa.api.entity.UserRole;
import com.luppa.api.entity.UserStatus;
import com.luppa.api.exception.ConflictException;
import com.luppa.api.exception.InvalidRequestException;
import com.luppa.api.exception.ResourceNotFoundException;
import com.luppa.api.repository.PendingRegistrationRepository;
import com.luppa.api.repository.RoleRepository;
import com.luppa.api.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class PendingRegistrationService {
    static final int CODE_EXPIRES_IN_SECONDS = 10 * 60;
    static final int RESEND_COOLDOWN_SECONDS = 60;
    static final int MAX_FAILED_ATTEMPTS = 5;

    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final CommunicationPreferenceService communicationPreferenceService;
    private final AccountEmailService accountEmailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public PendingRegistrationService(
            PendingRegistrationRepository pendingRegistrationRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            CommunicationPreferenceService communicationPreferenceService,
            AccountEmailService accountEmailService
    ) {
        this.pendingRegistrationRepository = pendingRegistrationRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.communicationPreferenceService = communicationPreferenceService;
        this.accountEmailService = accountEmailService;
    }

    @Transactional
    public RegisterStartResponse start(RegisterRequest request) {
        if (request.role().isPrivileged()) {
            throw new InvalidRequestException("Public registration cannot create privileged users");
        }
        if (!Boolean.TRUE.equals(request.termsAccepted())) {
            throw new InvalidRequestException("É necessário aceitar os Termos de Uso e a Política de Privacidade.");
        }
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email already registered");
        }

        Role role = roleRepository.findByName(request.role())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        PendingRegistration pending = pendingRegistrationRepository.findByEmail(email)
                .orElseGet(PendingRegistration::new);
        enforceCooldown(pending);

        String code = verificationCode();
        applyPendingRegistration(pending, request, email, role, code);
        pendingRegistrationRepository.save(pending);
        accountEmailService.registrationCode(request.name().trim(), email, code);
        return response(email);
    }

    @Transactional
    public RegisterStartResponse resend(RegisterResendRequest request) {
        String email = normalizeEmail(request.email());
        PendingRegistration pending = pendingRegistrationRepository.findByEmail(email).orElse(null);
        if (pending == null || userRepository.existsByEmailIgnoreCase(email)) {
            return response(email);
        }
        enforceCooldown(pending);
        String code = verificationCode();
        LocalDateTime now = LocalDateTime.now();
        pending.setCodeHash(passwordEncoder.encode(code));
        pending.setExpiresAt(now.plusSeconds(CODE_EXPIRES_IN_SECONDS));
        pending.setFailedAttempts(0);
        pending.setLastSentAt(now);
        pendingRegistrationRepository.save(pending);
        accountEmailService.registrationCode(pending.getName(), email, code);
        return response(email);
    }

    @Transactional
    public RegisterCompleteResponse verify(RegisterVerifyRequest request) {
        String email = normalizeEmail(request.email());
        PendingRegistration pending = pendingRegistrationRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidRequestException("Código inválido ou expirado."));
        LocalDateTime now = LocalDateTime.now();
        if (pending.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            throw new InvalidRequestException("Muitas tentativas inválidas. Solicite um novo código.");
        }
        if (pending.getExpiresAt().isBefore(now)) {
            throw new InvalidRequestException("Código inválido ou expirado.");
        }
        if (!passwordEncoder.matches(request.code(), pending.getCodeHash())) {
            pending.setFailedAttempts(pending.getFailedAttempts() + 1);
            throw new InvalidRequestException("Código inválido ou expirado.");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            pendingRegistrationRepository.delete(pending);
            throw new ConflictException("Email already registered");
        }

        User user = new User();
        user.setName(pending.getName());
        user.setEmail(email);
        user.setDateOfBirth(pending.getDateOfBirth());
        user.setPhone(pending.getPhone());
        user.setPasswordHash(pending.getPasswordHash());
        user.setRole(pending.getRole());
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(now);

        try {
            User saved = userRepository.saveAndFlush(user);
            communicationPreferenceService.createForRegistration(saved.getId(), true, pending.isAcceptedMarketing());
            pendingRegistrationRepository.delete(pending);
            try {
                accountEmailService.welcome(saved);
            } catch (IllegalStateException ignored) {
                // Welcome delivery should not undo a verified, completed registration.
            }
            return new RegisterCompleteResponse(email, true);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Email already registered");
        }
    }

    private void applyPendingRegistration(PendingRegistration pending, RegisterRequest request, String email, Role role, String code) {
        LocalDateTime now = LocalDateTime.now();
        pending.setName(request.name().trim());
        pending.setEmail(email);
        pending.setDateOfBirth(request.dateOfBirth());
        pending.setPhone(request.phone().trim());
        pending.setPasswordHash(passwordEncoder.encode(request.password()));
        pending.setRole(role);
        pending.setAcceptedMarketing(Boolean.TRUE.equals(request.marketingEmails()));
        pending.setCodeHash(passwordEncoder.encode(code));
        pending.setExpiresAt(now.plusSeconds(CODE_EXPIRES_IN_SECONDS));
        pending.setFailedAttempts(0);
        pending.setLastSentAt(now);
    }

    private void enforceCooldown(PendingRegistration pending) {
        LocalDateTime lastSentAt = pending.getLastSentAt();
        if (lastSentAt == null) {
            return;
        }
        long elapsed = Duration.between(lastSentAt, LocalDateTime.now()).toSeconds();
        if (elapsed < RESEND_COOLDOWN_SECONDS) {
            throw new InvalidRequestException("Aguarde antes de solicitar um novo código.");
        }
    }

    private RegisterStartResponse response(String email) {
        return new RegisterStartResponse(email, CODE_EXPIRES_IN_SECONDS, RESEND_COOLDOWN_SECONDS);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String verificationCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }
}
