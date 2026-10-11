package com.luppa.api.security;

import com.luppa.api.entity.Role;
import com.luppa.api.entity.User;
import com.luppa.api.entity.UserRole;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AuthUserRoleTest {
    @Test
    void luppaAdminInheritsAdminAuthority() {
        AuthUser authUser = new AuthUser(user(UserRole.LUPPA_ADMIN));

        assertThat(authUser.getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .contains("ROLE_LUPPA_ADMIN", "ROLE_ADMIN");
    }

    @Test
    void userAndCreatorDoNotReceiveAdminAuthority() {
        assertThat(new AuthUser(user(UserRole.USER)).getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .doesNotContain("ROLE_ADMIN");
        assertThat(new AuthUser(user(UserRole.CREATOR)).getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .doesNotContain("ROLE_ADMIN");
    }

    private User user(UserRole roleName) {
        User user = new User();
        setId(user, 10);
        user.setName("User Test");
        user.setEmail("user@luppa.com");
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
