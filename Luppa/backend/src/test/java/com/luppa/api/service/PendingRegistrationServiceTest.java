package com.luppa.api.service;

import com.luppa.api.dto.RegisterRequest;
import com.luppa.api.dto.RegisterResendRequest;
import com.luppa.api.dto.RegisterVerifyRequest;
import com.luppa.api.entity.PendingRegistration;
import com.luppa.api.entity.Role;
import com.luppa.api.entity.User;
import com.luppa.api.entity.UserRole;
import com.luppa.api.exception.ConflictException;
import com.luppa.api.exception.InvalidRequestException;
import com.luppa.api.repository.PendingRegistrationRepository;
import com.luppa.api.repository.RoleRepository;
import com.luppa.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
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

class PendingRegistrationServiceTest {
    private final PendingRegistrationRepository pendingRegistrationRepository = mock(PendingRegistrationRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final PasswordEncoder passwordEncoder = new TestPasswordEncoder();
    private final CommunicationPreferenceService communicationPreferenceService = mock(CommunicationPreferenceService.class);
    private final AccountEmailService accountEmailService = mock(AccountEmailService.class);
    private final PendingRegistrationService service = new PendingRegistrationService(
            pendingRegistrationRepository,
            userRepository,
            roleRepository,
            passwordEncoder,
            communicationPreferenceService,
            accountEmailService
    );

    @Test
    void startingRegistrationDoesNotCreateUserAndStoresNoPlaintextSecrets() {
        when(userRepository.existsByEmailIgnoreCase("user@luppa.com")).thenReturn(false);
        when(roleRepository.findByName(UserRole.USER)).thenReturn(Optional.of(role(UserRole.USER)));
        when(pendingRegistrationRepository.findByEmail("user@luppa.com")).thenReturn(Optional.empty());
        when(pendingRegistrationRepository.save(any(PendingRegistration.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<PendingRegistration> pendingCaptor = ArgumentCaptor.forClass(PendingRegistration.class);
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);

        service.start(registerRequest());

        verify(userRepository, never()).save(any());
        verify(userRepository, never()).saveAndFlush(any());
        verify(pendingRegistrationRepository).save(pendingCaptor.capture());
        verify(accountEmailService).registrationCode(eq("User Test"), eq("user@luppa.com"), codeCaptor.capture());

        PendingRegistration pending = pendingCaptor.getValue();
        assertThat(pending.getPasswordHash()).isNotEqualTo("password123");
        assertThat(pending.getCodeHash()).isNotEqualTo(codeCaptor.getValue());
        assertThat(passwordEncoder.matches("password123", pending.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches(codeCaptor.getValue(), pending.getCodeHash())).isTrue();
    }

    @Test
    void correctCodeCreatesVerifiedUserAndDeletesPendingRegistration() {
        PendingRegistration pending = pending("123456", LocalDateTime.now().plusMinutes(5), 0);
        when(pendingRegistrationRepository.findByEmail("user@luppa.com")).thenReturn(Optional.of(pending));
        when(userRepository.existsByEmailIgnoreCase("user@luppa.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            setId(user, 55);
            return user;
        });
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        service.verify(new RegisterVerifyRequest("user@luppa.com", "123456"));

        verify(userRepository).saveAndFlush(userCaptor.capture());
        User created = userCaptor.getValue();
        assertThat(created.getEmail()).isEqualTo("user@luppa.com");
        assertThat(created.isEmailVerified()).isTrue();
        assertThat(created.getEmailVerifiedAt()).isNotNull();
        assertThat(created.getPasswordHash()).isEqualTo(pending.getPasswordHash());
        verify(communicationPreferenceService).createForRegistration(55, true, true);
        verify(pendingRegistrationRepository).delete(pending);
    }

    @Test
    void invalidCodeDoesNotCreateUser() {
        PendingRegistration pending = pending("123456", LocalDateTime.now().plusMinutes(5), 0);
        when(pendingRegistrationRepository.findByEmail("user@luppa.com")).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.verify(new RegisterVerifyRequest("user@luppa.com", "000000")))
                .isInstanceOf(InvalidRequestException.class);

        assertThat(pending.getFailedAttempts()).isEqualTo(1);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void expiredCodeDoesNotCreateUser() {
        PendingRegistration pending = pending("123456", LocalDateTime.now().minusSeconds(1), 0);
        when(pendingRegistrationRepository.findByEmail("user@luppa.com")).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.verify(new RegisterVerifyRequest("user@luppa.com", "123456")))
                .isInstanceOf(InvalidRequestException.class);

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void excessiveFailedAttemptsAreRejected() {
        PendingRegistration pending = pending("123456", LocalDateTime.now().plusMinutes(5), 5);
        when(pendingRegistrationRepository.findByEmail("user@luppa.com")).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.verify(new RegisterVerifyRequest("user@luppa.com", "123456")))
                .isInstanceOf(InvalidRequestException.class);

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void resendReplacesPreviousCodeAndResetsAttempts() {
        PendingRegistration pending = pending("111111", LocalDateTime.now().plusMinutes(5), 3);
        pending.setLastSentAt(LocalDateTime.now().minusMinutes(2));
        when(pendingRegistrationRepository.findByEmail("user@luppa.com")).thenReturn(Optional.of(pending));
        when(userRepository.existsByEmailIgnoreCase("user@luppa.com")).thenReturn(false);
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);

        service.resend(new RegisterResendRequest("user@luppa.com"));

        verify(accountEmailService).registrationCode(eq("User Test"), eq("user@luppa.com"), codeCaptor.capture());
        assertThat(passwordEncoder.matches("111111", pending.getCodeHash())).isFalse();
        assertThat(passwordEncoder.matches(codeCaptor.getValue(), pending.getCodeHash())).isTrue();
        assertThat(pending.getFailedAttempts()).isZero();
        assertThat(pending.getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(9));
    }

    @Test
    void duplicateEmailAtStartIsRejectedSafely() {
        when(userRepository.existsByEmailIgnoreCase("user@luppa.com")).thenReturn(true);

        assertThatThrownBy(() -> service.start(registerRequest()))
                .isInstanceOf(ConflictException.class);

        verify(pendingRegistrationRepository, never()).save(any());
    }

    @Test
    void publicRegistrationCannotCreatePrivilegedRoles() {
        assertThatThrownBy(() -> service.start(registerRequest(UserRole.ADMIN)))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.start(registerRequest(UserRole.LUPPA_ADMIN)))
                .isInstanceOf(InvalidRequestException.class);

        verify(pendingRegistrationRepository, never()).save(any());
    }

    @Test
    void duplicateEmailAtVerificationIsRejectedSafely() {
        PendingRegistration pending = pending("123456", LocalDateTime.now().plusMinutes(5), 0);
        when(pendingRegistrationRepository.findByEmail("user@luppa.com")).thenReturn(Optional.of(pending));
        when(userRepository.existsByEmailIgnoreCase("user@luppa.com")).thenReturn(true);

        assertThatThrownBy(() -> service.verify(new RegisterVerifyRequest("user@luppa.com", "123456")))
                .isInstanceOf(ConflictException.class);

        verify(pendingRegistrationRepository).delete(pending);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void concurrentVerificationCannotCreateDuplicateUsers() {
        PendingRegistration pending = pending("123456", LocalDateTime.now().plusMinutes(5), 0);
        when(pendingRegistrationRepository.findByEmail("user@luppa.com")).thenReturn(Optional.of(pending));
        when(userRepository.existsByEmailIgnoreCase("user@luppa.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> service.verify(new RegisterVerifyRequest("user@luppa.com", "123456")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void emailDeliveryFailureAtStartDoesNotCreateCompletedUser() {
        when(userRepository.existsByEmailIgnoreCase("user@luppa.com")).thenReturn(false);
        when(roleRepository.findByName(UserRole.USER)).thenReturn(Optional.of(role(UserRole.USER)));
        when(pendingRegistrationRepository.findByEmail("user@luppa.com")).thenReturn(Optional.empty());
        when(pendingRegistrationRepository.save(any(PendingRegistration.class))).thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.doThrow(new IllegalStateException("email delivery failed"))
                .when(accountEmailService).registrationCode(eq("User Test"), eq("user@luppa.com"), any(String.class));

        assertThatThrownBy(() -> service.start(registerRequest()))
                .isInstanceOf(IllegalStateException.class);

        verify(userRepository, never()).saveAndFlush(any());
        verify(userRepository, never()).save(any());
    }

    private RegisterRequest registerRequest() {
        return registerRequest(UserRole.USER);
    }

    private RegisterRequest registerRequest(UserRole role) {
        return new RegisterRequest(
                "User Test",
                "user@luppa.com",
                LocalDate.of(1990, 1, 1),
                "11999999999",
                "password123",
                role,
                true,
                true
        );
    }

    private PendingRegistration pending(String code, LocalDateTime expiresAt, int failedAttempts) {
        PendingRegistration pending = new PendingRegistration();
        pending.setName("User Test");
        pending.setEmail("user@luppa.com");
        pending.setDateOfBirth(LocalDate.of(1990, 1, 1));
        pending.setPhone("11999999999");
        pending.setPasswordHash(passwordEncoder.encode("password123"));
        pending.setRole(role(UserRole.USER));
        pending.setAcceptedMarketing(true);
        pending.setCodeHash(passwordEncoder.encode(code));
        pending.setExpiresAt(expiresAt);
        pending.setFailedAttempts(failedAttempts);
        pending.setLastSentAt(LocalDateTime.now().minusMinutes(2));
        return pending;
    }

    private Role role(UserRole roleName) {
        Role role = new Role();
        role.setName(roleName);
        return role;
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

    private static final class TestPasswordEncoder implements PasswordEncoder {
        @Override
        public String encode(CharSequence rawPassword) {
            return "{test}" + Integer.toHexString(rawPassword.toString().hashCode());
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return encode(rawPassword).equals(encodedPassword);
        }
    }
}
