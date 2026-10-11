package com.luppa.api.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class ProductionSafetyValidator implements ApplicationRunner {
    private static final List<String> PLACEHOLDER_SECRETS = List.of(
            "CHANGE_ME_TO_A_256_BIT_SECRET_BEFORE_RUNNING_OUTSIDE_DEV",
            "replace-with-at-least-32-characters",
            "test-only-256-bit-secret-for-luppa-automated-tests"
    );

    private final ProductionSafetyProperties properties;
    private final JwtProperties jwtProperties;
    private final CorsProperties corsProperties;
    private final CloudinaryProperties cloudinaryProperties;
    private final EmailProperties emailProperties;
    private final Environment environment;

    public ProductionSafetyValidator(
            ProductionSafetyProperties properties,
            JwtProperties jwtProperties,
            CorsProperties corsProperties,
            CloudinaryProperties cloudinaryProperties,
            EmailProperties emailProperties,
            Environment environment
    ) {
        this.properties = properties;
        this.jwtProperties = jwtProperties;
        this.corsProperties = corsProperties;
        this.cloudinaryProperties = cloudinaryProperties;
        this.emailProperties = emailProperties;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.enabled()) {
            return;
        }
        requireStrongJwtSecret();
        requireNonLocalUrl("FRONTEND_URL/app.cors.allowed-origins", corsProperties.allowedOrigins());
        requireNonLocalUrl("APP_BASE_URL/app.email.app-base-url", emailProperties.appBaseUrl());
        requireNonLocalUrl("LUPPA_TERMS_URL/app.email.terms-url", emailProperties.termsUrl());
        requireNonLocalUrl("LUPPA_PRIVACY_URL/app.email.privacy-url", emailProperties.privacyUrl());
        requireDatabaseSafety();
        requireCloudinarySafety();
        requireEmailSafety();
    }

    private void requireStrongJwtSecret() {
        String secret = jwtProperties.secret();
        if (secret == null || secret.isBlank()) {
            fail("JWT_SECRET is required in production.");
        }
        if (PLACEHOLDER_SECRETS.contains(secret.trim())) {
            fail("JWT_SECRET uses a known placeholder value.");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            fail("JWT_SECRET must contain at least 32 bytes for HS256 signing.");
        }
    }

    private void requireDatabaseSafety() {
        requirePresent("DB_HOST");
        requirePresent("DB_NAME");
        requirePresent("DB_USERNAME");
        requirePresent("DB_PASSWORD");
        String dbHost = environment.getProperty("DB_HOST", "");
        if (isLocalHost(dbHost)) {
            fail("DB_HOST must not be localhost in production.");
        }
        String url = environment.getProperty("spring.datasource.url", "");
        if (!url.contains("encrypt=true")) {
            fail("SQL Server encryption must remain enabled in production.");
        }
        if (!url.contains("trustServerCertificate=false")) {
            fail("SQL Server certificate validation is required in production.");
        }
    }

    private void requireCloudinarySafety() {
        requirePresent("CLOUDINARY_CLOUD_NAME");
        requirePresent("CLOUDINARY_API_KEY");
        requirePresent("CLOUDINARY_API_SECRET");
        if (!cloudinaryProperties.isConfigured()) {
            fail("Cloudinary must be configured in production.");
        }
    }

    private void requireEmailSafety() {
        requirePresent("BREVO_API_KEY");
        requirePresent("LUPPA_EMAIL_FROM");
        requirePresent("LUPPA_SUPPORT_EMAIL");
    }

    private void requireNonLocalUrl(String name, List<String> values) {
        if (values == null || values.isEmpty()) {
            fail(name + " is required in production.");
        }
        values.forEach(value -> requireNonLocalUrl(name, value));
    }

    private void requireNonLocalUrl(String name, String value) {
        requireText(name, value);
        String normalized = value.trim().toLowerCase();
        if (!normalized.startsWith("https://")) {
            fail(name + " must use HTTPS in production.");
        }
        if (normalized.contains("localhost") || normalized.contains("127.0.0.1")) {
            fail(name + " must not point to localhost in production.");
        }
    }

    private void requirePresent(String key) {
        requireText(key, environment.getProperty(key));
    }

    private void requireText(String name, String value) {
        if (value == null || value.isBlank()) {
            fail(name + " is required in production.");
        }
    }

    private boolean isLocalHost(String host) {
        String normalized = host.trim().toLowerCase();
        return normalized.equals("localhost") || normalized.equals("127.0.0.1") || normalized.equals("::1");
    }

    private void fail(String message) {
        throw new IllegalStateException("Unsafe production configuration: " + message);
    }
}
