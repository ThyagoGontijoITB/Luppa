package com.luppa.api.controller;

import com.luppa.api.dto.EventInsightsResponse;
import com.luppa.api.dto.EventInsightsSummaryResponse;
import com.luppa.api.config.CorsProperties;
import com.luppa.api.config.JwtProperties;
import com.luppa.api.config.StorageProperties;
import com.luppa.api.exception.GlobalExceptionHandler;
import com.luppa.api.security.CustomUserDetailsService;
import com.luppa.api.security.JwtAuthenticationFilter;
import com.luppa.api.security.JwtService;
import com.luppa.api.security.SecurityConfig;
import com.luppa.api.service.EventService;
import com.luppa.api.service.SupportService;
import com.luppa.api.security.PublicEndpointRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {EventController.class, SupportController.class},
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
class EventWorkflowSecurityTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;
    @MockBean private SupportService supportService;
    @MockBean private PublicEndpointRateLimiter limiter;
    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void anonymousCannotReadMineEvenThoughEventReadsArePublic() throws Exception {
        mockMvc.perform(get("/api/events/mine")).andExpect(status().isForbidden());
        org.mockito.Mockito.verifyNoInteractions(eventService);
    }
    @Test @WithMockUser(roles = "USER")
    void ordinaryUserCannotReadMine() throws Exception {
        mockMvc.perform(get("/api/events/mine")).andExpect(status().isForbidden());
    }
    @Test
    void supportIsPublicAndAllowsConfiguredCorsOrigin() throws Exception {
        when(supportService.create(any(), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(new com.luppa.api.dto.SupportContactResponse(null, "accepted"));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/support/contact")
            .header("Origin", "http://localhost:5173")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Ana\",\"email\":\"ana@example.com\",\"category\":\"Contato\",\"subject\":\"Ajuda\",\"message\":\"Mensagem\"}"))
            .andExpect(status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
    @Test
    void invalidContactIsRejectedBeforeSending() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/support/contact")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Ana\",\"email\":\"invalid\",\"category\":\"Contato\",\"subject\":\"Ajuda\",\"message\":\"Mensagem\"}"))
            .andExpect(status().isBadRequest());
        org.mockito.Mockito.verifyNoInteractions(supportService);
    }
}
