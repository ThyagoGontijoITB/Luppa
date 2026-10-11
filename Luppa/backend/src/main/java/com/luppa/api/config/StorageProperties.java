package com.luppa.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String localDirectory,
        String publicBaseUrl,
        String profileLocalDirectory,
        String profilePublicBaseUrl
) {
}
