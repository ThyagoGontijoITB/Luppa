package com.luppa.api.service;

import com.luppa.api.dto.CommunicationPreferencesResponse;
import com.luppa.api.dto.CommunicationPreferencesUpdateRequest;
import com.luppa.api.entity.CommunicationPreference;
import com.luppa.api.entity.LegalAcceptance;
import com.luppa.api.entity.LegalDocumentType;
import com.luppa.api.entity.MarketingConsent;
import com.luppa.api.entity.User;
import com.luppa.api.entity.UserRole;
import com.luppa.api.exception.InvalidRequestException;
import com.luppa.api.repository.CommunicationPreferenceRepository;
import com.luppa.api.repository.LegalAcceptanceRepository;
import com.luppa.api.repository.MarketingConsentRepository;
import com.luppa.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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

class CommunicationPreferenceServiceTest {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final LegalAcceptanceRepository legalAcceptanceRepository = mock(LegalAcceptanceRepository.class);
    private final MarketingConsentRepository marketingConsentRepository = mock(MarketingConsentRepository.class);
    private final CommunicationPreferenceRepository communicationPreferenceRepository = mock(CommunicationPreferenceRepository.class);
    private final CommunicationPreferenceService service = new CommunicationPreferenceService(
            userRepository,
            legalAcceptanceRepository,
            marketingConsentRepository,
            communicationPreferenceRepository
    );

    @Test
    void registrationCreatesMandatoryLegalAcceptanceAndNoMarketingConsentWhenOptedOut() {
        User user = user();
        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        when(marketingConsentRepository.findById(10)).thenReturn(Optional.empty());
        when(communicationPreferenceRepository.findById(10)).thenReturn(Optional.empty());
        ArgumentCaptor<LegalAcceptance> legalCaptor = ArgumentCaptor.forClass(LegalAcceptance.class);

        service.createForRegistration(10, true, false);

        verify(legalAcceptanceRepository, org.mockito.Mockito.times(2)).save(legalCaptor.capture());
        assertThat(legalCaptor.getAllValues()).extracting(LegalAcceptance::getDocumentType)
                .containsExactlyInAnyOrder(LegalDocumentType.TERMS_OF_USE, LegalDocumentType.PRIVACY_POLICY);
        assertThat(legalCaptor.getAllValues()).extracting(acceptance -> acceptance.getDocumentType().databaseValue())
                .containsExactlyInAnyOrder("TERMOS_USO", "POLITICA_PRIVACIDADE");
        verify(marketingConsentRepository, never()).save(any(MarketingConsent.class));
        verify(communicationPreferenceRepository).save(any(CommunicationPreference.class));
    }

    @Test
    void registrationCreatesMarketingConsentWhenOptedIn() {
        User user = user();
        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        when(marketingConsentRepository.findById(10)).thenReturn(Optional.empty());
        when(communicationPreferenceRepository.findById(10)).thenReturn(Optional.empty());
        ArgumentCaptor<MarketingConsent> marketingCaptor = ArgumentCaptor.forClass(MarketingConsent.class);

        service.createForRegistration(10, true, true);

        verify(marketingConsentRepository).save(marketingCaptor.capture());
        assertThat(marketingCaptor.getValue().isAcceptedMarketing()).isTrue();
    }

    @Test
    void legalAcceptanceIsMandatory() {
        assertThatThrownBy(() -> service.createForRegistration(10, false, true))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void communicationPreferencesRemainIndependentFromMarketingConsent() {
        User user = user();
        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        when(marketingConsentRepository.findById(10)).thenReturn(Optional.empty());
        when(communicationPreferenceRepository.findById(10)).thenReturn(Optional.empty());
        when(legalAcceptanceRepository.existsByUserIdAndDocumentType(10, LegalDocumentType.TERMS_OF_USE)).thenReturn(true);
        when(legalAcceptanceRepository.existsByUserIdAndDocumentType(10, LegalDocumentType.PRIVACY_POLICY)).thenReturn(true);
        when(marketingConsentRepository.findById(10)).thenReturn(Optional.of(marketing(false)));
        when(communicationPreferenceRepository.findById(10)).thenReturn(Optional.of(preference(true, false, true)));

        CommunicationPreferencesResponse response = service.update(10, new CommunicationPreferencesUpdateRequest(false, true, false, true));

        assertThat(response.acceptedTerms()).isTrue();
        assertThat(response.marketingEmails()).isFalse();
        assertThat(response.luppaNews()).isTrue();
        assertThat(response.eventRecommendations()).isFalse();
        assertThat(response.savedEventReminders()).isTrue();
    }

    private MarketingConsent marketing(boolean accepted) {
        MarketingConsent consent = new MarketingConsent();
        consent.setUser(user());
        consent.setAcceptedMarketing(accepted);
        return consent;
    }

    private CommunicationPreference preference(boolean news, boolean recommendations, boolean reminders) {
        CommunicationPreference preference = new CommunicationPreference();
        preference.setUser(user());
        preference.setReceiveLuppaNews(news);
        preference.setReceiveEventRecommendations(recommendations);
        preference.setReceiveSavedEventReminders(reminders);
        return preference;
    }

    private User user() {
        com.luppa.api.entity.Role role = new com.luppa.api.entity.Role();
        role.setName(UserRole.USER);
        User user = new User();
        setId(user, 10);
        user.setName("User Test");
        user.setEmail("user@luppa.com");
        user.setPhone("11999999999");
        user.setDateOfBirth(LocalDate.of(1990, 1, 1));
        user.setPasswordHash("hash");
        user.setRole(role);
        return user;
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
