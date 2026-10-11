package com.luppa.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.geocoding")
public record GeocodingProperties(String provider, String baseUrl) {
}
