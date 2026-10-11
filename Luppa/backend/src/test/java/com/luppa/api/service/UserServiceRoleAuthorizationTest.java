package com.luppa.api.service;

import com.luppa.api.dto.UserCreateRequest;
import com.luppa.api.dto.UserUpdateRequest;
import com.luppa.api.config.StorageProperties;
import com.luppa.api.entity.Role;
import com.luppa.api.entity.User;
import com.luppa.api.entity.UserRole;
import com.luppa.api.entity.UserStatus;
import com.luppa.api.exception.ForbiddenOperationException;
import com.luppa.api.repository.RoleRepository;
import com.luppa.api.repository.UserDeletionRepository;
import com.luppa.api.repository.UserRepository;
import com.luppa.api.security.AuthUser;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceRoleAuthorizationTest {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final UserDeletionRepository userDeletionRepository = mock(UserDeletionRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final StorageProperties storageProperties = new StorageProperties("uploads/events", "", "uploads/profile", "");
    private final UserService service = new UserService(userRepository, roleRepository, userDeletionRepository, passwordEncoder, storageProperties);

    @Test
    void adminCannotDeleteLuppaAdmin() {
        User protectedUser = user(20, UserRole.LUPPA_ADMIN);
        when(userRepository.findById(20)).thenReturn(Optional.of(protectedUser));

        assertThatThrownBy(() -> service.delete(20, authUser(1, UserRole.ADMIN)))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(userRepository, never()).deleteById(20);
    }

    @Test
    void adminCannotSuspendLuppaAdmin() {
        User protectedUser = user(20, UserRole.LUPPA_ADMIN);
        when(userRepository.findById(20)).thenReturn(Optional.of(protectedUser));

        assertThatThrownBy(() -> service.update(20, updateRequest(UserRole.LUPPA_ADMIN, UserStatus.SUSPENDED), authUser(1, UserRole.ADMIN)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void adminCannotModifyProtectedLuppaAdminFields() {
        User protectedUser = user(20, UserRole.LUPPA_ADMIN);
        when(userRepository.findById(20)).thenReturn(Optional.of(protectedUser));

        assertThatThrownBy(() -> service.update(20, updateRequest(UserRole.LUPPA_ADMIN, UserStatus.ACTIVE), authUser(1, UserRole.ADMIN)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void adminCannotAssignLuppaAdmin() {
        User target = user(20, UserRole.USER);
        when(userRepository.findById(20)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.update(20, updateRequest(UserRole.LUPPA_ADMIN, UserStatus.ACTIVE), authUser(1, UserRole.ADMIN)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void adminCannotCreateRegularAdmin() {
        assertThatThrownBy(() -> service.create(createRequest(UserRole.ADMIN), authUser(1, UserRole.ADMIN)))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void adminCannotDeleteRegularAdmin() {
        User regularAdmin = user(20, UserRole.ADMIN);
        when(userRepository.findById(20)).thenReturn(Optional.of(regularAdmin));

        assertThatThrownBy(() -> service.delete(20, authUser(1, UserRole.ADMIN)))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(userRepository, never()).deleteById(20);
    }

    @Test
    void selfRoleChangeIsRejected() {
        User admin = user(1, UserRole.ADMIN);
        when(userRepository.findById(1)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.update(1, updateRequest(UserRole.USER, UserStatus.ACTIVE), new AuthUser(admin)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void luppaAdminCanManageRegularAdmin() {
        User regularAdmin = user(20, UserRole.ADMIN);
        when(userRepository.findById(20)).thenReturn(Optional.of(regularAdmin));
        when(userRepository.findByEmailIgnoreCase("updated@luppa.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName(UserRole.CREATOR)).thenReturn(Optional.of(role(UserRole.CREATOR)));

        service.update(20, updateRequest(UserRole.CREATOR, UserStatus.ACTIVE), authUser(1, UserRole.LUPPA_ADMIN));

        assertThat(regularAdmin.getRole().getName()).isEqualTo(UserRole.CREATOR);
    }

    @Test
    void protectedAccountsCannotBeDemotedByLuppaAdminThroughOrdinaryAdminEndpoint() {
        User protectedUser = user(20, UserRole.LUPPA_ADMIN);
        when(userRepository.findById(20)).thenReturn(Optional.of(protectedUser));

        assertThatThrownBy(() -> service.update(20, updateRequest(UserRole.ADMIN, UserStatus.ACTIVE), authUser(1, UserRole.LUPPA_ADMIN)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    private UserCreateRequest createRequest(UserRole role) {
        return new UserCreateRequest(
                "User Test",
                "created@luppa.com",
                LocalDate.of(1990, 1, 1),
                "11999999999",
                "password123",
                role,
                UserStatus.ACTIVE
        );
    }

    private UserUpdateRequest updateRequest(UserRole role, UserStatus status) {
        return new UserUpdateRequest(
                "Updated User",
                "updated@luppa.com",
                LocalDate.of(1990, 1, 1),
                "11999999999",
                role,
                status
        );
    }

    private AuthUser authUser(Integer id, UserRole role) {
        return new AuthUser(user(id, role));
    }

    private User user(Integer id, UserRole roleName) {
        User user = new User();
        setId(user, id);
        user.setName("User Test");
        user.setEmail("user" + id + "@luppa.com");
        user.setDateOfBirth(LocalDate.of(1990, 1, 1));
        user.setPhone("11999999999");
        user.setPasswordHash("hash");
        user.setRole(role(roleName));
        user.setStatus(UserStatus.ACTIVE);
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
