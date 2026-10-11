package com.luppa.api.service;

import com.luppa.api.dto.RegisterVerifyRequest;
import com.luppa.api.entity.PendingRegistration;
import com.luppa.api.entity.Role;
import com.luppa.api.entity.UserRole;
import com.luppa.api.repository.PendingRegistrationRepository;
import com.luppa.api.repository.RoleRepository;
import com.luppa.api.repository.UserRepository;
import com.luppa.api.support.RequiresIsolatedSqlServerTestDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(properties = {
        "app.bootstrap.admin.email=",
        "app.bootstrap.admin.password="
})
@ExtendWith(RequiresIsolatedSqlServerTestDatabase.class)
class PendingRegistrationRollbackIntegrationTest {
    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PendingRegistrationService pendingRegistrationService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockBean
    private CommunicationPreferenceService communicationPreferenceService;
    @MockBean
    private AccountEmailService accountEmailService;

    private String email;

    @AfterEach
    void cleanup() {
        if (email == null) {
            return;
        }
        transactionTemplate().executeWithoutResult(status -> {
            pendingRegistrationRepository.findByEmail(email).ifPresent(pendingRegistrationRepository::delete);
            userRepository.findByEmailIgnoreCase(email).ifPresent(userRepository::delete);
        });
    }

    @Test
    void rollsBackUserCreationWhenFinalRegistrationPreferencesFail() {
        email = "rollback-" + System.nanoTime() + "@luppa.test";
        pendingRegistrationRepository.saveAndFlush(pending(email));
        doThrow(new IllegalStateException("preference failure"))
                .when(communicationPreferenceService)
                .createForRegistration(anyInt(), anyBoolean(), anyBoolean());

        assertThatThrownBy(() -> pendingRegistrationService.verify(new RegisterVerifyRequest(email, "123456")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("preference failure");

        transactionTemplate().executeWithoutResult(status -> {
            assertThat(userRepository.findByEmailIgnoreCase(email)).isEmpty();
            assertThat(pendingRegistrationRepository.findByEmail(email)).isPresent();
        });
    }

    private PendingRegistration pending(String email) {
        PendingRegistration pending = new PendingRegistration();
        pending.setName("User Test");
        pending.setEmail(email);
        pending.setDateOfBirth(LocalDate.of(1990, 1, 1));
        pending.setPhone("11999999999");
        pending.setPasswordHash(passwordEncoder.encode("password123"));
        pending.setRole(userRole());
        pending.setAcceptedMarketing(true);
        pending.setCodeHash(passwordEncoder.encode("123456"));
        pending.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        pending.setFailedAttempts(0);
        pending.setLastSentAt(LocalDateTime.now().minusMinutes(2));
        return pending;
    }

    private Role userRole() {
        return roleRepository.findByName(UserRole.USER).orElseThrow();
    }

    private TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(transactionManager);
    }
}
