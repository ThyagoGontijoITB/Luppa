package com.luppa.api.service;

import com.luppa.api.dto.ForgotPasswordRequest;
import com.luppa.api.dto.ResendVerificationRequest;
import com.luppa.api.dto.ResetPasswordRequest;
import com.luppa.api.entity.EmailVerificationToken;
import com.luppa.api.entity.PasswordResetToken;
import com.luppa.api.entity.Role;
import com.luppa.api.entity.User;
import com.luppa.api.entity.UserRole;
import com.luppa.api.exception.InvalidRequestException;
import com.luppa.api.repository.EmailVerificationTokenRepository;
import com.luppa.api.repository.PasswordResetTokenRepository;
import com.luppa.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountTokenServiceTest {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AccountEmailService emailService = mock(AccountEmailService.class);
    private final EmailVerificationTokenRepository emailVerificationTokenRepository = mock(EmailVerificationTokenRepository.class);
    private final PasswordResetTokenRepository passwordResetTokenRepository = mock(PasswordResetTokenRepository.class);
    private final AccountTokenService service = new AccountTokenService(
            userRepository,
            passwordEncoder,
            emailService,
            emailVerificationTokenRepository,
            passwordResetTokenRepository
    );

    @Test
    void createsVerificationTokenWithOnlyHashPersisted() {
        User user = user(false);
        ArgumentCaptor<EmailVerificationToken> tokenCaptor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        ArgumentCaptor<String> rawCaptor = ArgumentCaptor.forClass(String.class);

        service.createVerification(user);

        verify(emailVerificationTokenRepository).save(tokenCaptor.capture());
        verify(emailService).verification(eq(user), rawCaptor.capture());
        assertThat(rawCaptor.getValue()).isNotBlank();
        assertThat(tokenCaptor.getValue().getTokenHash()).isEqualTo(sha256(rawCaptor.getValue()));
        assertThat(tokenCaptor.getValue().getTokenHash()).hasSize(32);
        assertThat(tokenCaptor.getValue().getDestinationEmail()).isEqualTo(user.getEmail());
        assertThat(tokenCaptor.getValue().getExpiresAt()).isAfter(LocalDateTime.now().plusHours(23));
    }

    @Test
    void verifiesEmailAndConsumesSingleUseToken() {
        String raw = "raw-verification-token";
        User user = user(false);
        EmailVerificationToken token = verificationToken(user, LocalDateTime.now().plusMinutes(10), null);
        when(emailVerificationTokenRepository.findFirstByTokenHash(sha256(raw))).thenReturn(Optional.of(token));

        service.verifyEmail(raw);

        assertThat(token.getConsumedAt()).isNotNull();
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getEmailVerifiedAt()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void rejectsExpiredVerificationToken() {
        String raw = "expired-verification-token";
        EmailVerificationToken token = verificationToken(user(false), LocalDateTime.now().minusMinutes(1), null);
        when(emailVerificationTokenRepository.findFirstByTokenHash(sha256(raw))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.verifyEmail(raw)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void rejectsReusedVerificationToken() {
        String raw = "reused-verification-token";
        EmailVerificationToken token = verificationToken(user(false), LocalDateTime.now().plusMinutes(10), LocalDateTime.now());
        when(emailVerificationTokenRepository.findFirstByTokenHash(sha256(raw))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.verifyEmail(raw)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void resendInvalidatesOpenVerificationTokensAndSendsNewToken() {
        User user = user(false);
        when(userRepository.findByEmailIgnoreCase("user@luppa.com")).thenReturn(Optional.of(user));

        service.resendVerification(new ResendVerificationRequest("user@luppa.com"));

        verify(emailVerificationTokenRepository).consumeOpenTokensForUser(eq(user.getId()), any(LocalDateTime.class));
        verify(emailVerificationTokenRepository).save(any(EmailVerificationToken.class));
        verify(emailService).verification(eq(user), any(String.class));
    }

    @Test
    void forgotPasswordDoesNotRevealMissingEmail() {
        when(userRepository.findByEmailIgnoreCase("missing@luppa.com")).thenReturn(Optional.empty());

        service.forgotPassword(new ForgotPasswordRequest("missing@luppa.com"));

        verify(passwordResetTokenRepository, never()).save(any());
        verify(emailService, never()).passwordReset(any(), any());
    }

    @Test
    void resetsPasswordAndConsumesToken() {
        String raw = "raw-reset-token";
        User user = user(true);
        PasswordResetToken token = resetToken(user, LocalDateTime.now().plusMinutes(10), null);
        when(passwordResetTokenRepository.findFirstByTokenHash(sha256(raw))).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-password");

        service.resetPassword(new ResetPasswordRequest(raw, "new-password"));

        assertThat(user.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(token.getConsumedAt()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void rejectsExpiredResetToken() {
        String raw = "expired-reset-token";
        PasswordResetToken token = resetToken(user(true), LocalDateTime.now().minusMinutes(1), null);
        when(passwordResetTokenRepository.findFirstByTokenHash(sha256(raw))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest(raw, "new-password")))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void rejectsReusedResetToken() {
        String raw = "reused-reset-token";
        PasswordResetToken token = resetToken(user(true), LocalDateTime.now().plusMinutes(10), LocalDateTime.now());
        when(passwordResetTokenRepository.findFirstByTokenHash(sha256(raw))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest(raw, "new-password")))
                .isInstanceOf(InvalidRequestException.class);
    }

    private User user(boolean verified) {
        Role role = new Role();
        role.setName(UserRole.USER);
        User user = new User();
        setId(user, 42);
        user.setName("User Test");
        user.setEmail("user@luppa.com");
        user.setPhone("11999999999");
        user.setDateOfBirth(LocalDate.of(1990, 1, 1));
        user.setPasswordHash("old-hash");
        user.setRole(role);
        user.setEmailVerified(verified);
        return user;
    }

    private EmailVerificationToken verificationToken(User user, LocalDateTime expiresAt, LocalDateTime consumedAt) {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setDestinationEmail(user.getEmail());
        token.setTokenHash(sha256("token"));
        token.setExpiresAt(expiresAt);
        token.setConsumedAt(consumedAt);
        return token;
    }

    private PasswordResetToken resetToken(User user, LocalDateTime expiresAt, LocalDateTime consumedAt) {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(sha256("token"));
        token.setExpiresAt(expiresAt);
        token.setConsumedAt(consumedAt);
        return token;
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void setId(User user, Integer id) {
        try {
            Field field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
