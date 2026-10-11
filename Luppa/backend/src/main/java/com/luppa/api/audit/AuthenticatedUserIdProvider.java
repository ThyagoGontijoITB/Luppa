package com.luppa.api.audit;

import com.luppa.api.security.AuthUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuthenticatedUserIdProvider {
    public Optional<Integer> currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        if (authentication.getPrincipal() instanceof AuthUser authUser) {
            return Optional.of(authUser.idUsuario());
        }

        return Optional.empty();
    }
}
