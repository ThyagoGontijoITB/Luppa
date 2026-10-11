package com.luppa.api.service;

import com.luppa.api.dto.RegisterVerifyRequest;
import com.luppa.api.entity.CommunicationPreference;
import com.luppa.api.entity.MarketingConsent;
import com.luppa.api.entity.PendingRegistration;
import com.luppa.api.entity.Role;
import com.luppa.api.entity.User;
import com.luppa.api.entity.UserRole;
import com.luppa.api.repository.CommunicationPreferenceRepository;
import com.luppa.api.repository.MarketingConsentRepository;
import com.luppa.api.repository.PendingRegistrationRepository;
import com.luppa.api.repository.RoleRepository;
import com.luppa.api.repository.UserRepository;
import com.luppa.api.support.RequiresIsolatedSqlServerTestDatabase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "app.bootstrap.admin.email=",
        "app.bootstrap.admin.password="
})
@Transactional
@ExtendWith(RequiresIsolatedSqlServerTestDatabase.class)
class SharedPrimaryKeyMappingIntegrationTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private MarketingConsentRepository marketingConsentRepository;
    @Autowired
    private CommunicationPreferenceRepository communicationPreferenceRepository;
    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;
    @Autowired
    private PendingRegistrationService pendingRegistrationService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EntityManager entityManager;

    @MockBean
    private AccountEmailService accountEmailService;

    @Test
    void mapsMarketingConsentAndCommunicationPreferenceToUserSharedPrimaryKey() {
        User user = user("shared-key-" + System.nanoTime() + "@luppa.test");
        User saved = userRepository.saveAndFlush(user);

        MarketingConsent consent = new MarketingConsent();
        consent.setUser(saved);
        consent.setAcceptedMarketing(true);
        MarketingConsent savedConsent = marketingConsentRepository.saveAndFlush(consent);

        CommunicationPreference preference = new CommunicationPreference();
        preference.setUser(saved);
        preference.setReceiveLuppaNews(true);
        preference.setReceiveEventRecommendations(true);
        preference.setReceiveSavedEventReminders(false);
        CommunicationPreference savedPreference = communicationPreferenceRepository.saveAndFlush(preference);

        entityManager.clear();

        assertThat(savedConsent.getUserId()).isEqualTo(saved.getId());
        assertThat(savedPreference.getUserId()).isEqualTo(saved.getId());
        assertThat(marketingConsentRepository.findById(saved.getId())).isPresent();
        assertThat(communicationPreferenceRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void verifiedRegistrationWithMarketingAcceptedPersistsConsentAndPreferences() {
        String email = "accepted-" + System.nanoTime() + "@luppa.test";
        pendingRegistrationRepository.saveAndFlush(pending(email, true));

        pendingRegistrationService.verify(new RegisterVerifyRequest(email, "123456"));
        entityManager.flush();
        entityManager.clear();

        User saved = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(marketingConsentRepository.findById(saved.getId()))
                .isPresent()
                .get()
                .extracting(MarketingConsent::isAcceptedMarketing)
                .isEqualTo(true);
        assertThat(communicationPreferenceRepository.findById(saved.getId()))
                .isPresent()
                .get()
                .satisfies(preference -> {
                    assertThat(preference.isReceiveLuppaNews()).isTrue();
                    assertThat(preference.isReceiveEventRecommendations()).isTrue();
                    assertThat(preference.isReceiveSavedEventReminders()).isFalse();
                });
        assertThat(pendingRegistrationRepository.findByEmail(email)).isEmpty();
    }

    @Test
    void verifiedRegistrationWithMarketingDeclinedPersistsPreferencesWithoutMarketingConsent() {
        String email = "declined-" + System.nanoTime() + "@luppa.test";
        pendingRegistrationRepository.saveAndFlush(pending(email, false));

        pendingRegistrationService.verify(new RegisterVerifyRequest(email, "123456"));
        entityManager.flush();
        entityManager.clear();

        User saved = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(marketingConsentRepository.findById(saved.getId())).isEmpty();
        assertThat(communicationPreferenceRepository.findById(saved.getId()))
                .isPresent()
                .get()
                .satisfies(preference -> {
                    assertThat(preference.isReceiveLuppaNews()).isFalse();
                    assertThat(preference.isReceiveEventRecommendations()).isFalse();
                    assertThat(preference.isReceiveSavedEventReminders()).isFalse();
                });
        assertThat(pendingRegistrationRepository.findByEmail(email)).isEmpty();
    }

    private User user(String email) {
        User user = new User();
        user.setName("User Test");
        user.setEmail(email);
        user.setDateOfBirth(LocalDate.of(1990, 1, 1));
        user.setPhone("11999999999");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole(userRole());
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        return user;
    }

    private PendingRegistration pending(String email, boolean acceptedMarketing) {
        PendingRegistration pending = new PendingRegistration();
        pending.setName("User Test");
        pending.setEmail(email);
        pending.setDateOfBirth(LocalDate.of(1990, 1, 1));
        pending.setPhone("11999999999");
        pending.setPasswordHash(passwordEncoder.encode("password123"));
        pending.setRole(userRole());
        pending.setAcceptedMarketing(acceptedMarketing);
        pending.setCodeHash(passwordEncoder.encode("123456"));
        pending.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        pending.setFailedAttempts(0);
        pending.setLastSentAt(LocalDateTime.now().minusMinutes(2));
        return pending;
    }

    private Role userRole() {
        return roleRepository.findByName(UserRole.USER).orElseThrow();
    }
}
