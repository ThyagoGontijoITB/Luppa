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
import com.luppa.api.service.EventInsightsService;
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
        controllers = EventInsightsController.class,
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
class EventInsightsControllerSecurityTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventInsightsService eventInsightsService;
    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(roles = "USER")
    void userCannotAccessCreatorInsights() throws Exception {
        mockMvc.perform(get("/api/insights/events/creator"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CREATOR")
    void creatorCannotAccessPlatformInsights() throws Exception {
        mockMvc.perform(get("/api/insights/events/platform"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanAccessPlatformInsights() throws Exception {
        when(eventInsightsService.platformInsights(any())).thenReturn(emptyResponse());

        mockMvc.perform(get("/api/insights/events/platform"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"LUPPA_ADMIN", "ADMIN"})
    void luppaAdminCanAccessPlatformInsights() throws Exception {
        when(eventInsightsService.platformInsights(any())).thenReturn(emptyResponse());

        mockMvc.perform(get("/api/insights/events/platform"))
                .andExpect(status().isOk());
    }

    private EventInsightsResponse emptyResponse() {
        return new EventInsightsResponse(
                new EventInsightsSummaryResponse(0, 0, 0, 0, 0, 0, 0, null),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}
