package com.luppa.api.service;

import com.luppa.api.dto.CommunicationPreferencesResponse;
import com.luppa.api.dto.CommunicationPreferencesUpdateRequest;
import com.luppa.api.entity.CommunicationPreference;
import com.luppa.api.entity.LegalAcceptance;
import com.luppa.api.entity.LegalDocumentType;
import com.luppa.api.entity.MarketingConsent;
import com.luppa.api.entity.User;
import com.luppa.api.exception.InvalidRequestException;
import com.luppa.api.exception.ResourceNotFoundException;
import com.luppa.api.repository.CommunicationPreferenceRepository;
import com.luppa.api.repository.LegalAcceptanceRepository;
import com.luppa.api.repository.MarketingConsentRepository;
import com.luppa.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunicationPreferenceService {
    static final LegalDocumentType TERMS_DOCUMENT = LegalDocumentType.TERMS_OF_USE;
    static final LegalDocumentType PRIVACY_DOCUMENT = LegalDocumentType.PRIVACY_POLICY;
    static final String CURRENT_LEGAL_VERSION = "2026-10-04";

    private final UserRepository userRepository;
    private final LegalAcceptanceRepository legalAcceptanceRepository;
    private final MarketingConsentRepository marketingConsentRepository;
    private final CommunicationPreferenceRepository communicationPreferenceRepository;

    public CommunicationPreferenceService(
            UserRepository userRepository,
            LegalAcceptanceRepository legalAcceptanceRepository,
            MarketingConsentRepository marketingConsentRepository,
            CommunicationPreferenceRepository communicationPreferenceRepository
    ) {
        this.userRepository = userRepository;
        this.legalAcceptanceRepository = legalAcceptanceRepository;
        this.marketingConsentRepository = marketingConsentRepository;
        this.communicationPreferenceRepository = communicationPreferenceRepository;
    }

    @Transactional
    public void createForRegistration(Integer userId, boolean termsAccepted, boolean marketingEmails) {
        if (!termsAccepted) {
            throw new InvalidRequestException("É necessário aceitar os Termos de Uso e a Política de Privacidade.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        createLegalAcceptance(user, TERMS_DOCUMENT);
        createLegalAcceptance(user, PRIVACY_DOCUMENT);
        if (marketingEmails) {
            upsertMarketingConsent(user, true);
        }
        upsertCommunicationPreference(user, marketingEmails, marketingEmails, false);
    }

    @Transactional(readOnly = true)
    public CommunicationPreferencesResponse get(Integer userId) {
        boolean acceptedTerms = legalAcceptanceRepository.existsByUserIdAndDocumentType(userId, TERMS_DOCUMENT)
                && legalAcceptanceRepository.existsByUserIdAndDocumentType(userId, PRIVACY_DOCUMENT);
        boolean marketingEmails = marketingConsentRepository.findById(userId)
                .map(MarketingConsent::isAcceptedMarketing)
                .orElse(false);
        return communicationPreferenceRepository.findById(userId)
                .map(preference -> new CommunicationPreferencesResponse(
                        acceptedTerms,
                        marketingEmails,
                        preference.isReceiveLuppaNews(),
                        preference.isReceiveEventRecommendations(),
                        preference.isReceiveSavedEventReminders()
                ))
                .orElseGet(() -> new CommunicationPreferencesResponse(acceptedTerms, marketingEmails, false, false, false));
    }

    @Transactional
    public CommunicationPreferencesResponse update(Integer userId, CommunicationPreferencesUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        upsertMarketingConsent(user, request.marketingEmails());
        upsertCommunicationPreference(user, request.luppaNews(), request.eventRecommendations(), request.savedEventReminders());
        return get(userId);
    }

    private void createLegalAcceptance(User user, LegalDocumentType documentType) {
        LegalAcceptance acceptance = new LegalAcceptance();
        acceptance.setUser(user);
        acceptance.setDocumentType(documentType);
        acceptance.setDocumentVersion(CURRENT_LEGAL_VERSION);
        legalAcceptanceRepository.save(acceptance);
    }

    private void upsertMarketingConsent(User user, boolean accepted) {
        MarketingConsent consent = marketingConsentRepository.findById(user.getId())
                .orElseGet(() -> {
                    MarketingConsent next = new MarketingConsent();
                    next.setUser(user);
                    return next;
                });
        consent.setAcceptedMarketing(accepted);
        marketingConsentRepository.save(consent);
    }

    private void upsertCommunicationPreference(User user, boolean luppaNews, boolean eventRecommendations, boolean savedEventReminders) {
        CommunicationPreference preference = communicationPreferenceRepository.findById(user.getId())
                .orElseGet(() -> {
                    CommunicationPreference next = new CommunicationPreference();
                    next.setUser(user);
                    return next;
                });
        preference.setReceiveLuppaNews(luppaNews);
        preference.setReceiveEventRecommendations(eventRecommendations);
        preference.setReceiveSavedEventReminders(savedEventReminders);
        communicationPreferenceRepository.save(preference);
    }
}
