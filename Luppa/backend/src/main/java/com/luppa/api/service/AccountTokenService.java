package com.luppa.api.service;

import com.luppa.api.dto.ForgotPasswordRequest;
import com.luppa.api.dto.ResetPasswordRequest;
import com.luppa.api.dto.ResendVerificationRequest;
import com.luppa.api.entity.EmailVerificationToken;
import com.luppa.api.entity.PasswordResetToken;
import com.luppa.api.entity.User;
import com.luppa.api.exception.InvalidRequestException;
import com.luppa.api.repository.EmailVerificationTokenRepository;
import com.luppa.api.repository.PasswordResetTokenRepository;
import com.luppa.api.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class AccountTokenService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountEmailService emailService;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public AccountTokenService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AccountEmailService emailService,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    @Transactional
    public void createVerification(User user) {
        if (user.isEmailVerified()) {
            return;
        }
        createVerificationTokenAndSend(user, true);
    }

    @Transactional
    public void resendVerification(ResendVerificationRequest request) {
        userRepository.findByEmailIgnoreCase(request.email())
                .filter(user -> !user.isEmailVerified())
                .ifPresent(user -> createVerificationTokenAndSend(user, true));
    }

    @Transactional
    public void verifyEmail(String token) {
        LocalDateTime now = LocalDateTime.now();
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findFirstByTokenHash(hash(token))
                .orElseThrow(() -> new InvalidRequestException("Token de verificação inválido ou expirado."));
        if (verificationToken.getConsumedAt() != null || verificationToken.getExpiresAt().isBefore(now)) {
            throw new InvalidRequestException("Token de verificação inválido ou expirado.");
        }
        verificationToken.setConsumedAt(now);
        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(now);
        userRepository.save(user);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmailIgnoreCase(request.email()).ifPresent(user -> {
            String token = token();
            passwordResetTokenRepository.consumeOpenTokensForUser(user.getId(), LocalDateTime.now());
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUser(user);
            resetToken.setTokenHash(hash(token));
            resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(15));
            passwordResetTokenRepository.save(resetToken);
            try {
                emailService.passwordReset(user, token);
            } catch (IllegalStateException exception) {
                resetToken.setConsumedAt(LocalDateTime.now());
            }
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        LocalDateTime now = LocalDateTime.now();
        PasswordResetToken resetToken = passwordResetTokenRepository.findFirstByTokenHash(hash(request.token()))
                .orElseThrow(() -> new InvalidRequestException("Token de redefinição inválido ou expirado."));
        if (resetToken.getConsumedAt() != null || resetToken.getExpiresAt().isBefore(now)) {
            throw new InvalidRequestException("Token de redefinição inválido ou expirado.");
        }
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);
        resetToken.setConsumedAt(now);
    }

    private String token() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void createVerificationTokenAndSend(User user, boolean invalidateOpenTokens) {
        LocalDateTime now = LocalDateTime.now();
        if (invalidateOpenTokens) {
            emailVerificationTokenRepository.consumeOpenTokensForUser(user.getId(), now);
        }
        String token = token();
        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setUser(user);
        verificationToken.setDestinationEmail(user.getEmail());
        verificationToken.setTokenHash(hash(token));
        verificationToken.setExpiresAt(now.plusHours(24));
        emailVerificationTokenRepository.save(verificationToken);
        emailService.verification(user, token);
    }

    private byte[] hash(String token) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
