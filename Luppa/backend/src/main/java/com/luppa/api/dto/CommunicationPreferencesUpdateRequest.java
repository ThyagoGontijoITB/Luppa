package com.luppa.api.dto;

public record CommunicationPreferencesUpdateRequest(
        boolean marketingEmails,
        boolean luppaNews,
        boolean eventRecommendations,
        boolean savedEventReminders
) {
}
