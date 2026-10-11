package com.luppa.api.security;

import com.luppa.api.config.JwtProperties;
import com.luppa.api.config.CorsProperties;
import com.luppa.api.config.StorageProperties;
import com.luppa.api.controller.EventController;
import com.luppa.api.exception.GlobalExceptionHandler;
import com.luppa.api.service.EventService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = EventController.class,
        properties = {
                "app.jwt.secret=test-only-256-bit-secret-for-luppa-automated-tests",
                "app.jwt.expiration-minutes=60",
                "app.cors.allowed-origins=http://localhost:5173",
                "app.storage.local-directory=${java.io.tmpdir}/luppa-test/uploads/events",
                "app.storage.public-base-url=http://localhost:8081/uploads/events",
                "app.storage.profile-local-directory=${java.io.tmpdir}/luppa-test/uploads/profile",
                "app.storage.profile-public-base-url=http://localhost:8081/uploads/profile",
                "springdoc.api-docs.enabled=false",
                "springdoc.swagger-ui.enabled=false"
})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, GlobalExceptionHandler.class})
@EnableConfigurationProperties({CorsProperties.class, JwtProperties.class, StorageProperties.class})
class JwtAuthenticationFilterIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtProperties jwtProperties;

    @MockBean
    private EventService eventService;
    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void featuredEndpointWithExpiredTokenReturnsUnauthorizedApiError() throws Exception {
        String token = Jwts.builder()
                .subject("admin@luppa.test")
                .issuedAt(Date.from(Instant.now().minusSeconds(7200)))
                .expiration(Date.from(Instant.now().minusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8)))
                .compact();

        mockMvc.perform(put("/api/events/1/featured")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid or expired authentication token"))
                .andExpect(jsonPath("$.path").value("/api/events/1/featured"));
    }
}
