package com.luppa.api.service;

import com.luppa.api.dto.LoginRequest;
import com.luppa.api.dto.LoginResponse;
import com.luppa.api.entity.Role;
import com.luppa.api.entity.User;
import com.luppa.api.entity.UserRole;
import com.luppa.api.exception.ForbiddenOperationException;
import com.luppa.api.repository.UserRepository;
import com.luppa.api.security.AuthUser;
import com.luppa.api.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final AuthService service = new AuthService(authenticationManager, jwtService, userRepository);

    @Test
    void existingVerifiedUserLoginGetsJwt() {
        User user = user(UserRole.USER, true);
        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(userRepository.findByEmailIgnoreCase("user@luppa.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any(AuthUser.class))).thenReturn("jwt-token");

        LoginResponse response = service.login(new LoginRequest("user@luppa.com", "password"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.requiresEmailVerification()).isFalse();
        assertThat(response.user().emailVerified()).isTrue();
    }

    @Test
    void unverifiedUserLoginDoesNotGetNormalAccess() {
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("disabled"));

        assertThatThrownBy(() -> service.login(new LoginRequest("user@luppa.com", "password")))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    private User user(UserRole roleName, boolean verified) {
        User user = new User();
        setId(user, 10);
        user.setName("User Test");
        user.setEmail("user@luppa.com");
        user.setDateOfBirth(LocalDate.of(1990, 1, 1));
        user.setPhone("11999999999");
        user.setPasswordHash("hash");
        user.setRole(role(roleName));
        user.setEmailVerified(verified);
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
