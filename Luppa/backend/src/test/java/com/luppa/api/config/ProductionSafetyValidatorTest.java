package com.luppa.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionSafetyValidatorTest {
    @Test
    void acceptsConfiguredCloudinaryWithoutLegacyLocalStorageSettings() {
        ProductionSafetyValidator validator = validator(
                new CloudinaryProperties("luppa-prod", "api-key", "api-secret", "luppa"),
                productionEnvironment()
        );

        assertThatCode(() -> validator.run(new DefaultApplicationArguments()))
                .doesNotThrowAnyException();
    }

    @Test
    void stillRejectsMissingCloudinaryCredentials() {
        MockEnvironment environment = productionEnvironment();
        ProductionSafetyValidator validator = validator(
                new CloudinaryProperties("luppa-prod", "", "api-secret", "luppa"),
                environment
        );

        assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cloudinary must be configured in production");
    }

    private ProductionSafetyValidator validator(
            CloudinaryProperties cloudinaryProperties,
            MockEnvironment environment
    ) {
        return new ProductionSafetyValidator(
                new ProductionSafetyProperties(true),
                new JwtProperties("strong-test-secret-with-more-than-32-bytes", 120),
                new CorsProperties(List.of("https://frontend.example")),
                cloudinaryProperties,
                new EmailProperties(
                        "no-reply@example.com",
                        "",
                        "https://frontend.example",
                        "https://frontend.example/terms",
                        "https://frontend.example/privacy",
                        "support@example.com"
                ),
                environment
        );
    }

    private MockEnvironment productionEnvironment() {
        return new MockEnvironment()
                .withProperty("DB_HOST", "db.example")
                .withProperty("DB_NAME", "luppaBD")
                .withProperty("DB_USERNAME", "luppa_app")
                .withProperty("DB_PASSWORD", "not-real")
                .withProperty("spring.datasource.url", "jdbc:sqlserver://db.example:1433;databaseName=luppaBD;encrypt=true;trustServerCertificate=false")
                .withProperty("CLOUDINARY_CLOUD_NAME", "luppa-prod")
                .withProperty("CLOUDINARY_API_KEY", "api-key")
                .withProperty("CLOUDINARY_API_SECRET", "api-secret")
                .withProperty("MAIL_HOST", "smtp.example")
                .withProperty("MAIL_USERNAME", "smtp-user")
                .withProperty("MAIL_PASSWORD", "smtp-password")
                .withProperty("LUPPA_EMAIL_FROM", "no-reply@example.com")
                .withProperty("LUPPA_SUPPORT_EMAIL", "support@example.com");
    }
}
