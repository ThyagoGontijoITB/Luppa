package com.luppa.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luppa.api.config.BrevoProperties;
import com.luppa.api.config.EmailProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class BrevoTransactionalEmailSenderContextTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class)
            .withPropertyValues(
                    "app.brevo.api-key=test-brevo-api-key",
                    "app.brevo.endpoint=http://localhost:9/v3/smtp/email",
                    "app.email.from=Luppa <no-reply@luppa.com>",
                    "app.email.logo-url=",
                    "app.email.app-base-url=https://app.luppa.com",
                    "app.email.terms-url=https://app.luppa.com/terms",
                    "app.email.privacy-url=https://app.luppa.com/privacy",
                    "app.email.support-to=support@luppa.com"
            );

    @Test
    void contextCreatesBrevoTransactionalEmailSenderWithConfiguredConstructor() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(BrevoTransactionalEmailSender.class);
            assertThat(context).hasSingleBean(TransactionalEmailSender.class);
            assertThat(context).hasSingleBean(AccountEmailService.class);
        });
    }

    @Configuration
    @EnableConfigurationProperties({BrevoProperties.class, EmailProperties.class})
    @Import({
            AccountEmailService.class,
            BrevoTransactionalEmailSender.class,
            LuppaEmailTemplateService.class
    })
    static class TestConfiguration {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
