package com.luppa.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.brevo")
public record BrevoProperties(
        String apiKey,
        String endpoint,
        Duration connectTimeout,
        Duration requestTimeout
) {
    private static final String DEFAULT_ENDPOINT = "https://api.brevo.com/v3/smtp/email";
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(10);

    public BrevoProperties {
        if (endpoint == null || endpoint.isBlank()) {
            endpoint = DEFAULT_ENDPOINT;
        }
        if (connectTimeout == null) {
            connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        }
        if (requestTimeout == null) {
            requestTimeout = DEFAULT_REQUEST_TIMEOUT;
        }
    }
}
