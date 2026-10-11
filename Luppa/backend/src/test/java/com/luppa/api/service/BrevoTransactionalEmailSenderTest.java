package com.luppa.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luppa.api.config.BrevoProperties;
import com.luppa.api.config.EmailProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BrevoTransactionalEmailSenderTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsTransactionalEmailThroughBrevoRestApi() throws Exception {
        AtomicReference<String> apiKey = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        server = startServer(exchange -> {
            apiKey.set(exchange.getRequestHeaders().getFirst("api-key"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 201, "{\"messageId\":\"<ok@brevo>\"}");
        });

        sender("secret-brevo-key", "/v3/smtp/email", Duration.ofSeconds(2))
                .send(new TransactionalEmailMessage(
                        "user@luppa.com",
                        "Confirme seu e-mail",
                        "<html>codigo</html>",
                        "codigo"
                ));

        JsonNode json = objectMapper.readTree(body.get());
        assertThat(apiKey.get()).isEqualTo("secret-brevo-key");
        assertThat(json.get("sender").get("email").asText()).isEqualTo("no-reply@luppa.com");
        assertThat(json.get("sender").get("name").asText()).isEqualTo("Luppa");
        assertThat(json.get("to").get(0).get("email").asText()).isEqualTo("user@luppa.com");
        assertThat(json.get("subject").asText()).isEqualTo("Confirme seu e-mail");
        assertThat(json.get("htmlContent").asText()).isEqualTo("<html>codigo</html>");
        assertThat(json.get("textContent").asText()).isEqualTo("codigo");
    }

    @Test
    void failsWithoutBrevoApiKey() {
        assertThatThrownBy(() -> sender("", "/v3/smtp/email", Duration.ofSeconds(1))
                .send(new TransactionalEmailMessage("user@luppa.com", "Assunto", "<html></html>", "")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BREVO_API_KEY");
    }

    @Test
    void mapsBrevoHttpErrorsWithoutLeakingApiKey() throws Exception {
        server = startServer(exchange -> respond(exchange, 401, "{\"message\":\"unauthorized\"}"));

        assertThatThrownBy(() -> sender("secret-brevo-key", "/v3/smtp/email", Duration.ofSeconds(2))
                .send(new TransactionalEmailMessage("user@luppa.com", "Assunto", "<html></html>", "")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTP 401")
                .hasMessageNotContaining("secret-brevo-key");
    }

    @Test
    void mapsRequestTimeoutsWithoutLeakingApiKey() throws Exception {
        server = startServer(exchange -> {
            try {
                Thread.sleep(250);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            respond(exchange, 201, "{\"messageId\":\"<ok@brevo>\"}");
        });

        assertThatThrownBy(() -> sender("secret-brevo-key", "/v3/smtp/email", Duration.ofMillis(50))
                .send(new TransactionalEmailMessage("user@luppa.com", "Assunto", "<html></html>", "")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("API da Brevo")
                .hasMessageNotContaining("secret-brevo-key");
    }

    private BrevoTransactionalEmailSender sender(String apiKey, String path, Duration requestTimeout) {
        String endpoint = server == null
                ? "http://localhost:9" + path
                : "http://localhost:" + server.getAddress().getPort() + path;
        return new BrevoTransactionalEmailSender(
                new BrevoProperties(apiKey, endpoint, Duration.ofSeconds(1), requestTimeout),
                new EmailProperties(
                        "Luppa <no-reply@luppa.com>",
                        "",
                        "https://app.luppa.com",
                        "https://app.luppa.com/terms",
                        "https://app.luppa.com/privacy",
                        "support@luppa.com"
                ),
                objectMapper
        );
    }

    private HttpServer startServer(ExchangeHandler handler) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        httpServer.createContext("/v3/smtp/email", exchange -> {
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
        httpServer.start();
        return httpServer;
    }

    private void respond(HttpExchange exchange, int status, String responseBody) throws IOException {
        byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
