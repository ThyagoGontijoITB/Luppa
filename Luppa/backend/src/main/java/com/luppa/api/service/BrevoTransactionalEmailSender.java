package com.luppa.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luppa.api.config.BrevoProperties;
import com.luppa.api.config.EmailProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BrevoTransactionalEmailSender implements TransactionalEmailSender {
    private final BrevoProperties brevoProperties;
    private final EmailProperties emailProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public BrevoTransactionalEmailSender(
            BrevoProperties brevoProperties,
            EmailProperties emailProperties,
            ObjectMapper objectMapper
    ) {
        this(
                brevoProperties,
                emailProperties,
                objectMapper,
                HttpClient.newBuilder()
                        .connectTimeout(brevoProperties.connectTimeout())
                        .build()
        );
    }

    BrevoTransactionalEmailSender(
            BrevoProperties brevoProperties,
            EmailProperties emailProperties,
            ObjectMapper objectMapper,
            HttpClient httpClient
    ) {
        this.brevoProperties = brevoProperties;
        this.emailProperties = emailProperties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public void send(TransactionalEmailMessage message) {
        String apiKey = brevoProperties.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("BREVO_API_KEY is required to send transactional emails.");
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(brevoProperties.endpoint()))
                .timeout(requestTimeout())
                .header("accept", "application/json")
                .header("content-type", "application/json")
                .header("api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(toJson(message)))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Não foi possível enviar o e-mail transacional. Brevo retornou HTTP "
                        + response.statusCode() + ".");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível enviar o e-mail transacional pela API da Brevo.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Envio de e-mail transacional interrompido.", exception);
        }
    }

    private String toJson(TransactionalEmailMessage message) {
        try {
            return objectMapper.writeValueAsString(payload(message));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Não foi possível preparar o e-mail transacional.", exception);
        }
    }

    private Map<String, Object> payload(TransactionalEmailMessage message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sender", sender());
        payload.put("to", List.of(Map.of("email", message.to())));
        payload.put("subject", message.subject());
        payload.put("htmlContent", message.html());
        payload.put("textContent", message.text() == null ? "" : message.text());
        return payload;
    }

    private Map<String, String> sender() {
        String from = emailProperties.from() == null ? "" : emailProperties.from().trim();
        int start = from.lastIndexOf('<');
        int end = from.lastIndexOf('>');
        if (start >= 0 && end > start) {
            String name = from.substring(0, start).trim();
            String email = from.substring(start + 1, end).trim();
            if (!name.isBlank()) {
                return Map.of("email", email, "name", stripQuotes(name));
            }
            return Map.of("email", email);
        }
        return Map.of("email", from);
    }

    private String stripQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private Duration requestTimeout() {
        Duration timeout = brevoProperties.requestTimeout();
        return timeout.isNegative() || timeout.isZero() ? Duration.ofSeconds(10) : timeout;
    }
}
