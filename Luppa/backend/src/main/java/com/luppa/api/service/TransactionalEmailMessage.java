package com.luppa.api.service;

public record TransactionalEmailMessage(
        String to,
        String subject,
        String html,
        String text
) {
}
