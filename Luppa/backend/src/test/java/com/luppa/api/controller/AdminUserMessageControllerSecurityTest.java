package com.luppa.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luppa.api.dto.AdminUserMessageRequest;
import com.luppa.api.config.CorsProperties;
import com.luppa.api.config.JwtProperties;
import com.luppa.api.config.StorageProperties;
import com.luppa.api.exception.GlobalExceptionHandler;
import com.luppa.api.security.CustomUserDetailsService;
import com.luppa.api.security.JwtAuthenticationFilter;
import com.luppa.api.security.JwtService;
import com.luppa.api.security.SecurityConfig;
import com.luppa.api.service.AdminUserMessageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AdminUserMessageController.class,
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
class AdminUserMessageControllerSecurityTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminUserMessageService adminUserMessageService;
    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(roles = "USER")
    void userCannotAccessAdminMessageEndpoint() throws Exception {
        mockMvc.perform(post("/api/admin/users/20/message")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CREATOR")
    void creatorCannotAccessAdminMessageEndpoint() throws Exception {
        mockMvc.perform(post("/api/admin/users/20/message")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isForbidden());
    }

    private AdminUserMessageRequest request() {
        return new AdminUserMessageRequest("Informação", "Mensagem válida para envio.");
    }
}
