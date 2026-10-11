package com.luppa.api.config;

import com.luppa.api.entity.Role;
import com.luppa.api.entity.User;
import com.luppa.api.entity.UserRole;
import com.luppa.api.entity.UserStatus;
import com.luppa.api.repository.RoleRepository;
import com.luppa.api.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrap implements CommandLineRunner {
    private final AdminBootstrapProperties properties;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrap(
            AdminBootstrapProperties properties,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (properties.email() == null || properties.email().isBlank()
                || properties.password() == null || properties.password().isBlank()
                || userRepository.existsByEmailIgnoreCase(properties.email())) {
            return;
        }

        Role adminRole = roleRepository.findByName(UserRole.ADMIN)
                .orElseThrow(() -> new IllegalStateException("ADMIN role was not migrated"));

        User admin = new User();
        admin.setName(properties.name());
        admin.setEmail(properties.email().toLowerCase());
        admin.setPhone(properties.phone());
        admin.setDateOfBirth(properties.dateOfBirth());
        admin.setPasswordHash(passwordEncoder.encode(properties.password()));
        admin.setRole(adminRole);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setEmailVerified(true);
        admin.setEmailVerifiedAt(java.time.LocalDateTime.now());
        userRepository.save(admin);
    }
}
