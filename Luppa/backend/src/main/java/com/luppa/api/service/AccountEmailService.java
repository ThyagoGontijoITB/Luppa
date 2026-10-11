package com.luppa.api.service;

import com.luppa.api.config.EmailProperties;
import com.luppa.api.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class AccountEmailService {
    private final EmailProperties properties;
    private final LuppaEmailTemplateService templates;
    private final JavaMailSender mailSender;

    public AccountEmailService(EmailProperties properties, LuppaEmailTemplateService templates, JavaMailSender mailSender) {
        this.properties = properties;
        this.templates = templates;
        this.mailSender = mailSender;
    }

    public void passwordReset(User user, String token) {
        String url = properties.appBaseUrl() + "/reset-password?token=" + token;
        String body = templates.paragraph("Olá, " + user.getName() + "!")
                + templates.paragraph("Recebemos uma solicitação para redefinir a senha da sua conta Luppa.");
        String secondary = "Este link expira em 15 minutos.<br>Se você não solicitou esta alteração, ignore este e-mail.";
        send(user.getEmail(), "Redefinição de senha", templates.render("Redefinição de senha", body, "Redefinir minha senha", url, secondary),
                "Recebemos uma solicitação para redefinir a senha da sua conta Luppa.");
    }

    public void verification(User user, String token) {
        String url = properties.appBaseUrl() + "/verify-email?token=" + token;
        String body = templates.paragraph("Olá, " + user.getName() + "!")
                + templates.paragraph("Só falta confirmar que este endereço de e-mail pertence a você.");
        send(user.getEmail(), "Confirme seu e-mail", templates.render("Confirme seu e-mail", body, "Confirmar meu e-mail", url, "Este link expira em 24 horas."),
                "Confirme seu e-mail para ativar sua conta Luppa.");
    }

    public void registrationCode(String name, String email, String code) {
        String body = templates.paragraph("Olá, " + name + "!")
                + templates.paragraph("Use o código abaixo para confirmar seu e-mail e concluir seu cadastro na Luppa.")
                + templates.codeBlock(code);
        send(email, "Seu código de verificação Luppa", templates.render("Código de verificação", body, null, null, "Este código expira em 10 minutos."),
                "Seu código de verificação Luppa expira em 10 minutos.");
    }

    public void welcome(User user) {
        String body = templates.paragraph("Sua conta está pronta.")
                + templates.paragraph("Descubra eventos, salve seus favoritos e encontre o que está acontecendo perto de você.");
        send(user.getEmail(), "Bem-vindo à Luppa!", templates.render("Bem-vindo à Luppa!", body, "Explorar eventos", properties.appBaseUrl() + "/events", ""),
                "Bem-vindo à Luppa.");
    }

    public void supportConfirmation(String name, String email, String category, String subject) {
        String body = templates.paragraph("Olá, " + name + "!")
                + templates.paragraph("Sua solicitação foi enviada para a equipe Luppa.")
                + "<p style=\"margin:0 0 8px;\"><strong>Categoria:</strong><br>" + templates.escape(category) + "</p>"
                + "<p style=\"margin:0;\"><strong>Assunto:</strong><br>" + templates.escape(subject) + "</p>";
        send(email, "Recebemos sua mensagem", templates.render("Recebemos sua mensagem", body, null, null, "Nossa equipe irá analisar sua mensagem."),
                "Recebemos sua mensagem.");
    }

    public void eventPublished(User creator, Integer eventId, String eventTitle) {
        String body = templates.paragraph("\"" + eventTitle + "\" foi aprovado e já está disponível na Luppa.");
        send(creator.getEmail(), "Seu evento foi publicado!",
                templates.render("Seu evento foi publicado!", body, "Ver evento", properties.appBaseUrl() + "/events/" + eventId, ""),
                "Seu evento foi publicado.");
    }

    public void eventRejected(User creator, String eventTitle) {
        String body = templates.paragraph("Seu evento \"" + eventTitle + "\" não foi aprovado.");
        send(creator.getEmail(), "Atualização sobre seu evento",
                templates.render("Atualização sobre seu evento", body, null, null, ""),
                "Atualização sobre seu evento.");
    }

    public void reportUnderReview(User reporter, String eventTitle) {
        String body = templates.paragraph("Recebemos sua denúncia relacionada ao evento \"" + eventTitle + "\" e nossa equipe iniciou a análise.");
        send(reporter.getEmail(), "Sua denúncia está em análise",
                templates.render("Sua denúncia está em análise", body, null, null, "Nenhuma ação adicional é necessária no momento."),
                "Sua denúncia está em análise.");
    }

    public void reportResolved(User reporter, String eventTitle, String responseMessage) {
        String body = templates.paragraph("A análise da sua denúncia relacionada ao evento \"" + eventTitle + "\" foi concluída.")
                + moderationResponseBlock(responseMessage);
        send(reporter.getEmail(), "Sua denúncia foi analisada",
                templates.render("Sua denúncia foi analisada", body, null, null, ""),
                "Sua denúncia foi analisada.\n\nResposta da moderação:\n" + nullToBlank(responseMessage));
    }

    public void reportDismissed(User reporter, String eventTitle, String responseMessage) {
        String body = templates.paragraph("A análise da denúncia relacionada ao evento \"" + eventTitle + "\" foi encerrada.")
                + moderationResponseBlock(responseMessage);
        send(reporter.getEmail(), "Atualização sobre sua denúncia",
                templates.render("Atualização sobre sua denúncia", body, null, null, ""),
                "Atualização sobre sua denúncia.\n\nResposta da moderação:\n" + nullToBlank(responseMessage));
    }

    public void internalSupport(String name, String email, Integer userId, String category, String subject, String message, String timestamp) {
        String html = templates.render("Nova solicitação de suporte",
                "<p><strong>Nome:</strong><br>" + templates.escape(name) + "</p>"
                        + "<p><strong>E-mail:</strong><br>" + templates.escape(email) + "</p>"
                        + "<p><strong>Usuário:</strong><br>" + (userId == null ? "Não autenticado" : userId) + "</p>"
                        + "<p><strong>Categoria:</strong><br>" + templates.escape(category) + "</p>"
                        + "<p><strong>Assunto:</strong><br>" + templates.escape(subject) + "</p>"
                        + "<p><strong>Mensagem:</strong><br>" + templates.escape(message).replace("\n", "<br>") + "</p>"
                        + "<p><strong>Data:</strong><br>" + templates.escape(timestamp) + "</p>",
                null, null, "");
        send(properties.supportTo(), "[Luppa Suporte] " + subject, html, message);
    }

    public void adminUserMessage(User recipient, String subject, String message) {
        String safeMessage = templates.escape(message).replace("\n", "<br>");
        String body = templates.paragraph("Olá, " + recipient.getName() + "!")
                + "<p style=\"margin:0 0 8px;\"><strong>Assunto:</strong><br>" + templates.escape(subject) + "</p>"
                + "<div style=\"margin:14px 0 0;border:1px solid #E5E7EB;border-radius:8px;background:#F9FAFB;padding:14px;color:#111111;line-height:1.6;\">"
                + safeMessage
                + "</div>";
        String footer = "Mensagem enviada pela equipe Luppa. Em caso de dúvidas, responda este e-mail ou entre em contato pelo suporte: "
                + templates.escape(properties.supportTo()) + ".";
        String text = "Mensagem da equipe Luppa\n\n"
                + "Olá, " + recipient.getName() + "!\n\n"
                + "Assunto: " + subject + "\n\n"
                + message + "\n\n"
                + "Em caso de dúvidas, entre em contato pelo suporte: " + properties.supportTo();
        send(recipient.getEmail(), subject, templates.render("Mensagem da equipe Luppa", body, null, null, footer), text);
    }

    private void send(String to, String subject, String html, String text) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );
            helper.setFrom(properties.from());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text == null ? "" : text, html);
            if (templates.usesInlineLogo()) {
                helper.addInline(
                        LuppaEmailTemplateService.INLINE_LOGO_CONTENT_ID,
                        new ClassPathResource("email/luppa-wordmark-banner.png"),
                        "image/png"
                );
            }
            mailSender.send(message);
        } catch (MessagingException | MailException exception) {
            throw new IllegalStateException("Não foi possível enviar o e-mail transacional.", exception);
        }
    }

    private String moderationResponseBlock(String responseMessage) {
        String escaped = templates.escape(responseMessage).replace("\n", "<br>");
        return "<p style=\"margin:16px 0 8px;\"><strong>Resposta da moderação:</strong></p>"
                + "<div style=\"margin:0;border:1px solid #E5E7EB;border-radius:8px;background:#F9FAFB;padding:14px;color:#111111;line-height:1.6;\">"
                + escaped
                + "</div>";
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }
}
