package com.luppa.api.service;

import com.luppa.api.config.EmailProperties;
import com.luppa.api.entity.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AccountEmailServiceTest {
    private final EmailProperties properties = new EmailProperties(
            "Luppa <no-reply@luppa.com>",
            "",
            "https://app.luppa.com",
            "https://app.luppa.com/terms",
            "https://app.luppa.com/privacy",
            "support@luppa.com"
    );
    private final LuppaEmailTemplateService templates = new LuppaEmailTemplateService(properties);
    private final TransactionalEmailSender emailSender = mock(TransactionalEmailSender.class);
    private final AccountEmailService service = new AccountEmailService(properties, templates, emailSender);

    @Test
    void sendsEveryAccountEmailWithHtmlAndPlainTextAlternative() {
        User user = user();

        service.passwordReset(user, "reset-token");
        service.verification(user, "verification-token");
        service.registrationCode("User Test", "user@luppa.com", "123456");
        service.welcome(user);
        service.supportConfirmation("User Test", "user@luppa.com", "Conta", "Ajuda");
        service.eventPublished(user, 42, "Show Luppa");
        service.eventRejected(user, "Show Luppa");
        service.reportUnderReview(user, "Show Luppa");
        service.reportResolved(user, "Show Luppa", "Providências tomadas.");
        service.reportDismissed(user, "Show Luppa", "Não identificamos violação.");
        service.internalSupport("User Test", "user@luppa.com", 7, "Conta", "Ajuda", "Mensagem", "2026-10-04T12:00:00");
        service.adminUserMessage(user, "Informação da conta", "Mensagem administrativa.");

        ArgumentCaptor<TransactionalEmailMessage> messageCaptor = ArgumentCaptor.forClass(TransactionalEmailMessage.class);
        verify(emailSender, atLeastOnce()).send(messageCaptor.capture());
        List<TransactionalEmailMessage> messages = messageCaptor.getAllValues();

        assertThat(messages).hasSize(12);
        for (TransactionalEmailMessage message : messages) {
            assertThat(message.to()).isNotBlank();
            assertThat(message.subject()).isNotBlank();
            assertThat(message.html()).contains("<!doctype html>");
            assertThat(message.html()).contains("cid:luppa-email-logo");
            assertThat(message.text()).isNotNull();
        }
    }

    @Test
    void adminUserMessageEscapesHtmlInput() {
        service.adminUserMessage(user(), "<script>alert('x')</script>", "Linha 1\n<img src=x onerror=alert(1)>");

        ArgumentCaptor<TransactionalEmailMessage> messageCaptor = ArgumentCaptor.forClass(TransactionalEmailMessage.class);
        verify(emailSender).send(messageCaptor.capture());
        String html = messageCaptor.getValue().html();

        assertThat(html).doesNotContain("<script>");
        assertThat(html).doesNotContain("<img src=x");
        assertThat(html).contains("&lt;script&gt;");
        assertThat(html).contains("&lt;img");
        assertThat(html).contains("Linha 1<br>");
    }

    private User user() {
        User user = new User();
        user.setName("User Test");
        user.setEmail("user@luppa.com");
        return user;
    }
}
