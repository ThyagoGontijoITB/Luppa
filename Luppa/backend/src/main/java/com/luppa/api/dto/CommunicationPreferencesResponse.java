package com.luppa.api.dto;

public record CommunicationPreferencesResponse(
        boolean acceptedTerms,
        boolean marketingEmails,
        boolean luppaNews,
        boolean eventRecommendations,
        boolean savedEventReminders
) {
}
