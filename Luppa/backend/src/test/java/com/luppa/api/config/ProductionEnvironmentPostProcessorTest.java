package com.luppa.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionEnvironmentPostProcessorTest {
    private final ProductionEnvironmentPostProcessor processor = new ProductionEnvironmentPostProcessor();

    @Test
    void rejectsProductionPlaceholderJwtSecretBeforeApplicationContextStarts() {
        MockEnvironment environment = productionEnvironment()
                .withProperty("JWT_SECRET", "CHANGE_ME_TO_A_256_BIT_SECRET_BEFORE_RUNNING_OUTSIDE_DEV");

        assertThatThrownBy(() -> processor.postProcessEnvironment(environment, new SpringApplication()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET uses a known placeholder value");
    }

    @Test
    void rejectsProductionLocalhostUrls() {
        MockEnvironment environment = productionEnvironment()
                .withProperty("JWT_SECRET", "strong-test-secret-with-more-than-32-bytes")
                .withProperty("FRONTEND_URL", "http://localhost:5173");

        assertThatThrownBy(() -> processor.postProcessEnvironment(environment, new SpringApplication()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FRONTEND_URL must use HTTPS");
    }

    @Test
    void acceptsProductionCloudinaryConfigurationWithoutLegacyLocalStorageUrls() {
        MockEnvironment environment = productionEnvironment();

        assertThatCode(() -> processor.postProcessEnvironment(environment, new SpringApplication()))
                .doesNotThrowAnyException();
    }

    private MockEnvironment productionEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        return environment
                .withProperty("JWT_SECRET", "strong-test-secret-with-more-than-32-bytes")
                .withProperty("FRONTEND_URL", "https://frontend.example")
                .withProperty("APP_BASE_URL", "https://frontend.example")
                .withProperty("LUPPA_TERMS_URL", "https://frontend.example/terms")
                .withProperty("LUPPA_PRIVACY_URL", "https://frontend.example/privacy")
                .withProperty("DB_HOST", "db.example")
                .withProperty("DB_NAME", "luppaBD")
                .withProperty("DB_USERNAME", "luppa_app")
                .withProperty("DB_PASSWORD", "not-real")
                .withProperty("CLOUDINARY_CLOUD_NAME", "luppa-test")
                .withProperty("CLOUDINARY_API_KEY", "not-real")
                .withProperty("CLOUDINARY_API_SECRET", "not-real")
                .withProperty("BREVO_API_KEY", "brevo-api-key")
                .withProperty("LUPPA_EMAIL_FROM", "no-reply@example.com")
                .withProperty("LUPPA_SUPPORT_EMAIL", "support@example.com");
    }
}
