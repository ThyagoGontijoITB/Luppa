package com.luppa.api.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class ProductionEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    private static final List<String> PLACEHOLDER_SECRETS = List.of(
            "CHANGE_ME_TO_A_256_BIT_SECRET_BEFORE_RUNNING_OUTSIDE_DEV",
            "replace-with-at-least-32-characters",
            "replace-with-at-least-32-random-bytes",
            "test-only-256-bit-secret-for-luppa-automated-tests"
    );

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!isProduction(environment)) {
            return;
        }
        requireStrongJwtSecret(environment);
        requireNonLocalHttps(environment, "FRONTEND_URL");
        requireNonLocalHttps(environment, "APP_BASE_URL");
        requireNonLocalHttps(environment, "LUPPA_TERMS_URL");
        requireNonLocalHttps(environment, "LUPPA_PRIVACY_URL");
        requireNonLocalHttps(environment, "IMAGE_PUBLIC_BASE_URL");
        requireNonLocalHttps(environment, "PROFILE_IMAGE_PUBLIC_BASE_URL");
        requirePresent(environment, "DB_HOST");
        requirePresent(environment, "DB_NAME");
        requirePresent(environment, "DB_USERNAME");
        requirePresent(environment, "DB_PASSWORD");
        if (isLocalHost(environment.getProperty("DB_HOST", ""))) {
            fail("DB_HOST must not be localhost in production.");
        }
        requirePresent(environment, "CLOUDINARY_CLOUD_NAME");
        requirePresent(environment, "CLOUDINARY_API_KEY");
        requirePresent(environment, "CLOUDINARY_API_SECRET");
        requirePresent(environment, "MAIL_HOST");
        requirePresent(environment, "MAIL_USERNAME");
        requirePresent(environment, "MAIL_PASSWORD");
        requirePresent(environment, "LUPPA_EMAIL_FROM");
        requirePresent(environment, "LUPPA_SUPPORT_EMAIL");
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private boolean isProduction(ConfigurableEnvironment environment) {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod")
                || environment.getProperty("app.production-safety.enabled", Boolean.class, false);
    }

    private void requireStrongJwtSecret(ConfigurableEnvironment environment) {
        String secret = environment.getProperty("JWT_SECRET");
        if (secret == null) {
            secret = environment.getProperty("app.jwt.secret", "");
        }
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

    private void requireNonLocalHttps(ConfigurableEnvironment environment, String key) {
        String value = requirePresent(environment, key);
        Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .forEach(item -> {
                    String normalized = item.toLowerCase();
                    if (!normalized.startsWith("https://")) {
                        fail(key + " must use HTTPS in production.");
                    }
                    if (normalized.contains("localhost") || normalized.contains("127.0.0.1")) {
                        fail(key + " must not point to localhost in production.");
                    }
                });
    }

    private String requirePresent(ConfigurableEnvironment environment, String key) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) {
            fail(key + " is required in production.");
        }
        return value;
    }

    private boolean isLocalHost(String host) {
        String normalized = host.trim().toLowerCase();
        return normalized.equals("localhost") || normalized.equals("127.0.0.1") || normalized.equals("::1");
    }

    private void fail(String message) {
        throw new IllegalStateException("Unsafe production configuration: " + message);
    }
}
