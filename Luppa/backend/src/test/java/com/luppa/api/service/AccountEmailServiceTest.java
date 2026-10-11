package com.luppa.api.service;

import com.luppa.api.config.EmailProperties;
import com.luppa.api.entity.User;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final AccountEmailService service = new AccountEmailService(properties, templates, mailSender);

    @Test
    void sendsEveryAccountEmailAsMultipartHtmlWithPlainTextAlternative() throws Exception {
        when(mailSender.createMimeMessage()).thenAnswer(invocation ->
                new MimeMessage(Session.getInstance(new Properties()))
        );
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

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, atLeastOnce()).send(messageCaptor.capture());
        List<MimeMessage> messages = messageCaptor.getAllValues();

        assertThat(messages).hasSize(12);
        for (MimeMessage message : messages) {
            message.saveChanges();
            assertThat(message.isMimeType("multipart/*")).isTrue();
            assertThat(hasMimePart(message.getContent(), "text/plain")).isTrue();
            assertThat(hasMimePart(message.getContent(), "text/html")).isTrue();
            assertThat(hasMimePart(message.getContent(), "image/png")).isTrue();
            assertThat(findMimePart(message.getContent(), "text/html")).contains("cid:luppa-email-logo");
        }
    }

    @Test
    void adminUserMessageEscapesHtmlInput() throws Exception {
        when(mailSender.createMimeMessage()).thenAnswer(invocation ->
                new MimeMessage(Session.getInstance(new Properties()))
        );

        service.adminUserMessage(user(), "<script>alert('x')</script>", "Linha 1\n<img src=x onerror=alert(1)>");

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        MimeMessage message = messageCaptor.getValue();
        message.saveChanges();
        String html = findMimePart(message.getContent(), "text/html");

        assertThat(html).doesNotContain("<script>");
        assertThat(html).doesNotContain("<img src=x");
        assertThat(html).contains("&lt;script&gt;");
        assertThat(html).contains("&lt;img");
        assertThat(html).contains("Linha 1<br>");
    }

    private boolean hasMimePart(Object content, String mimeType) throws Exception {
        return findMimePart(content, mimeType) != null;
    }

    private String findMimePart(Object content, String mimeType) throws Exception {
        if (!(content instanceof Multipart multipart)) {
            return null;
        }
        for (int index = 0; index < multipart.getCount(); index++) {
            BodyPart part = multipart.getBodyPart(index);
            if (part.isMimeType(mimeType)) {
                return String.valueOf(part.getContent());
            }
            if (part.isMimeType("multipart/*") && hasMimePart(part.getContent(), mimeType)) {
                return findMimePart(part.getContent(), mimeType);
            }
        }
        return null;
    }

    private User user() {
        User user = new User();
        user.setName("User Test");
        user.setEmail("user@luppa.com");
        return user;
    }
}
