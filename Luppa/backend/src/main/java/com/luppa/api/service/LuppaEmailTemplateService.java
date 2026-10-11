package com.luppa.api.service;

import com.luppa.api.config.EmailProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
public class LuppaEmailTemplateService {
    private static final String BLUE = "#009CFF";
    private static final String DARK = "#111111";
    private static final String MUTED = "#667085";
    private static final String BORDER = "#E5E7EB";
    static final String INLINE_LOGO_CONTENT_ID = "luppa-email-logo";
    private final EmailProperties properties;

    public LuppaEmailTemplateService(EmailProperties properties) {
        this.properties = properties;
    }

    public String render(String title, String bodyHtml, String ctaLabel, String ctaUrl, String secondaryHtml) {
        String logo = logoHtml();
        String cta = ctaLabel == null || ctaUrl == null || ctaUrl.isBlank() ? "" : """
                <tr><td align="center" style="padding:18px 0 8px;">
                  <a href="%s" style="display:inline-block;background:%s;color:#FFFFFF;text-decoration:none;font-weight:700;border-radius:8px;padding:13px 22px;">%s</a>
                </td></tr>
                """.formatted(escape(ctaUrl), BLUE, escape(ctaLabel));
        String secondary = secondaryHtml == null || secondaryHtml.isBlank() ? "" : """
                <tr><td style="color:%s;font-size:14px;line-height:1.6;padding-top:12px;">%s</td></tr>
                """.formatted(MUTED, secondaryHtml);

        return """
                <!doctype html>
                <html lang="pt-BR">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>%s</title></head>
                <body style="margin:0;background:#F6F9FC;font-family:Arial,Helvetica,sans-serif;color:%s;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#F6F9FC;padding:28px 12px;">
                    <tr><td align="center">
                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:620px;background:#FFFFFF;border-radius:8px;border:1px solid %s;">
                        <tr><td align="center" style="background:%s;padding:20px 30px 18px;border-radius:8px 8px 0 0;">%s</td></tr>
                        <tr><td style="padding:28px 30px 28px;">
                          <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                            <tr><td style="font-size:24px;line-height:1.25;font-weight:800;color:%s;padding-bottom:16px;">%s</td></tr>
                            <tr><td style="font-size:16px;line-height:1.7;color:%s;">%s</td></tr>
                            %s
                            %s
                          </table>
                        </td></tr>
                        <tr><td style="background:#FAFBFC;border-top:1px solid %s;padding:18px 30px;color:%s;font-size:13px;line-height:1.6;">
                          <strong style="color:%s;">Luppa</strong><br>
                          <a href="%s" style="color:%s;text-decoration:none;">Termos de Uso</a>
                          <span style="color:#98A2B3;"> | </span>
                          <a href="%s" style="color:%s;text-decoration:none;">Política de Privacidade</a><br>
                          Este é um e-mail transacional da Luppa.
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(
                escape(title), DARK, BORDER, BLUE, logo,
                DARK, escape(title), DARK, bodyHtml, cta, secondary,
                BORDER, MUTED, DARK, escape(properties.termsUrl()), BLUE, escape(properties.privacyUrl()), BLUE
        );
    }

    private String logoHtml() {
        String logoUrl = properties.logoUrl();
        if (logoUrl == null || logoUrl.isBlank() || !logoUrl.startsWith("https://")) {
            return """
                    <img src="cid:%s" alt="Luppa" width="132" style="display:block;border:0;max-width:132px;width:132px;height:auto;">
                    """.formatted(INLINE_LOGO_CONTENT_ID);
        }
        return """
                <img src="%s" alt="Luppa" width="132" style="display:block;border:0;max-width:132px;width:132px;height:auto;">
                """.formatted(escape(logoUrl));
    }

    public boolean usesInlineLogo() {
        String logoUrl = properties.logoUrl();
        return logoUrl == null || logoUrl.isBlank() || !logoUrl.startsWith("https://");
    }

    public String paragraph(String value) {
        return "<p style=\"margin:0 0 14px;\">" + escape(value) + "</p>";
    }

    public String codeBlock(String code) {
        return """
                <div style="margin:22px 0 8px;text-align:center;">
                  <div style="display:inline-block;background:#F0F8FF;border:1px solid #B9E3FF;border-radius:8px;color:#111111;font-size:32px;font-weight:800;letter-spacing:8px;padding:15px 20px;">%s</div>
                </div>
                """.formatted(escape(code));
    }

    public String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }
}
