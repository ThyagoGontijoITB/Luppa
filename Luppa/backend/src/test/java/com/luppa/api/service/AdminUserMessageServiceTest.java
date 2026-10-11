package com.luppa.api.service;

import com.luppa.api.dto.AdminUserMessageRequest;
import com.luppa.api.entity.Role;
import com.luppa.api.entity.User;
import com.luppa.api.entity.UserRole;
import com.luppa.api.exception.ForbiddenOperationException;
import com.luppa.api.exception.InvalidRequestException;
import com.luppa.api.exception.ResourceNotFoundException;
import com.luppa.api.repository.UserRepository;
import com.luppa.api.security.AuthUser;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserMessageServiceTest {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final AccountEmailService accountEmailService = mock(AccountEmailService.class);
    private final AdminUserMessageRateLimiter rateLimiter = new AdminUserMessageRateLimiter();
    private final AdminUserMessageService service = new AdminUserMessageService(userRepository, accountEmailService, rateLimiter);

    @Test
    void adminCanMessageOrdinaryUsersAndRecipientEmailIsResolvedFromDatabase() {
        User target = user(20, UserRole.USER, "real-user@luppa.com");
        when(userRepository.findById(20)).thenReturn(Optional.of(target));
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        service.send(20, request("Informação sobre sua conta", "Olá! Estamos entrando em contato."), authUser(1, UserRole.ADMIN));

        verify(accountEmailService).adminUserMessage(userCaptor.capture(), org.mockito.Mockito.eq("Informação sobre sua conta"), org.mockito.Mockito.anyString());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("real-user@luppa.com");
    }

    @Test
    void luppaAdminCanMessageRegularAdmins() {
        User target = user(20, UserRole.ADMIN, "admin@luppa.com");
        when(userRepository.findById(20)).thenReturn(Optional.of(target));

        service.send(20, request("Informação sobre sua conta", "Olá! Estamos entrando em contato."), authUser(1, UserRole.LUPPA_ADMIN));

        verify(accountEmailService).adminUserMessage(target, "Informação sobre sua conta", "Olá! Estamos entrando em contato.");
    }

    @Test
    void adminCannotMessageLuppaAdmin() {
        User target = user(20, UserRole.LUPPA_ADMIN, "protected@luppa.com");
        when(userRepository.findById(20)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.send(20, request("Informação sobre sua conta", "Olá! Estamos entrando em contato."), authUser(1, UserRole.ADMIN)))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(accountEmailService, never()).adminUserMessage(org.mockito.Mockito.any(), org.mockito.Mockito.anyString(), org.mockito.Mockito.anyString());
    }

    @Test
    void protectedAccountsCannotBeTargetsForLuppaAdminEither() {
        User target = user(20, UserRole.LUPPA_ADMIN, "protected@luppa.com");
        when(userRepository.findById(20)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.send(20, request("Informação sobre sua conta", "Olá! Estamos entrando em contato."), authUser(1, UserRole.LUPPA_ADMIN)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void invalidRecipientReturnsNotFound() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.send(99, request("Informação sobre sua conta", "Olá! Estamos entrando em contato."), authUser(1, UserRole.ADMIN)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void blankSubjectIsRejected() {
        assertThatThrownBy(() -> service.send(20, request("     ", "Olá! Estamos entrando em contato."), authUser(1, UserRole.ADMIN)))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void blankMessageIsRejected() {
        assertThatThrownBy(() -> service.send(20, request("Informação", "     "), authUser(1, UserRole.ADMIN)))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void oversizedInputsAreRejected() {
        String longSubject = "A".repeat(151);
        String longMessage = "B".repeat(5001);

        assertThatThrownBy(() -> service.send(20, request(longSubject, "Mensagem válida"), authUser(1, UserRole.ADMIN)))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.send(20, request("Assunto válido", longMessage), authUser(1, UserRole.ADMIN)))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void smtpFailureIsHandledSafely() {
        User target = user(20, UserRole.USER, "real-user@luppa.com");
        when(userRepository.findById(20)).thenReturn(Optional.of(target));
        doThrow(new IllegalStateException("smtp credentials leaked detail"))
                .when(accountEmailService).adminUserMessage(target, "Informação", "Mensagem válida para envio.");

        assertThatThrownBy(() -> service.send(20, request("Informação", "Mensagem válida para envio."), authUser(1, UserRole.ADMIN)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Não foi possível enviar a mensagem no momento. Tente novamente mais tarde.");
    }

    @Test
    void rapidDuplicateSubmissionsAreLimited() {
        User target = user(20, UserRole.USER, "real-user@luppa.com");
        when(userRepository.findById(20)).thenReturn(Optional.of(target));
        AdminUserMessageRequest request = request("Informação", "Mensagem válida para envio.");
        AuthUser admin = authUser(1, UserRole.ADMIN);

        service.send(20, request, admin);

        assertThatThrownBy(() -> service.send(20, request, admin))
                .isInstanceOf(InvalidRequestException.class);
    }

    private AdminUserMessageRequest request(String subject, String message) {
        return new AdminUserMessageRequest(subject, message);
    }

    private AuthUser authUser(Integer id, UserRole roleName) {
        return new AuthUser(user(id, roleName, "actor" + id + "@luppa.com"));
    }

    private User user(Integer id, UserRole roleName, String email) {
        User user = new User();
        setId(user, id);
        user.setName("User Test");
        user.setEmail(email);
        user.setDateOfBirth(LocalDate.of(1990, 1, 1));
        user.setPhone("11999999999");
        user.setPasswordHash("hash");
        user.setRole(role(roleName));
        user.setEmailVerified(true);
        return user;
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
}
