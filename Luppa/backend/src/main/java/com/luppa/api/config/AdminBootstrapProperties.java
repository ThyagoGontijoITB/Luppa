package com.luppa.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalDate;

@ConfigurationProperties(prefix = "app.bootstrap.admin")
public record AdminBootstrapProperties(
        String email,
        String password,
        String name,
        String phone,
        LocalDate dateOfBirth
) {
}
