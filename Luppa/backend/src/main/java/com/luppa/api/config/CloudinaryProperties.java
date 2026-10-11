package com.luppa.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cloudinary")
public record CloudinaryProperties(
        String cloudName,
        String apiKey,
        String apiSecret,
        String rootFolder
) {
    public String normalizedRootFolder() {
        if (rootFolder == null || rootFolder.isBlank()) {
            return "luppa";
        }
        return rootFolder.replaceAll("^/+", "").replaceAll("/+$", "");
    }

    public boolean isConfigured() {
        return hasText(cloudName) && hasText(apiKey) && hasText(apiSecret);
    }

    public String deliveryUrl(String publicId) {
        return "https://res.cloudinary.com/" + cloudName + "/image/upload/" + publicId;
    }

    public boolean isManagedPublicId(String value) {
        return value != null && value.startsWith(normalizedRootFolder() + "/");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
