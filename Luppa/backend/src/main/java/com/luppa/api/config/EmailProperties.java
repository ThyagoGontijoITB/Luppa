package com.luppa.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.email")
public record EmailProperties(
        String from,
        String logoUrl,
        String appBaseUrl,
        String termsUrl,
        String privacyUrl,
        String supportTo
) {
}
