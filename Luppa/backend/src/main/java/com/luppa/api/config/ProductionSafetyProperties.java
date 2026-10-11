package com.luppa.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.production-safety")
public record ProductionSafetyProperties(boolean enabled) {
}
