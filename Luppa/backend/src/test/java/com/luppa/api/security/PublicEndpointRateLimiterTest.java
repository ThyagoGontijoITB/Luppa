package com.luppa.api.security;

import com.luppa.api.config.RateLimitProperties;
import com.luppa.api.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PublicEndpointRateLimiterTest {
    @Test
    void limitsRepeatedLoginByAccountAndIp() {
        PublicEndpointRateLimiter limiter = limiter(List.of());
        MockHttpServletRequest request = request("203.0.113.10");

        for (int i = 0; i < 8; i++) {
            limiter.checkLogin(request, "USER@LUPPA.TEST");
        }

        assertThatThrownBy(() -> limiter.checkLogin(request, "user@luppa.test"))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void ignoresForwardedForFromUntrustedRemoteAddress() {
        PublicEndpointRateLimiter limiter = limiter(List.of("10.0.0.0/8"));

        for (int i = 0; i < 20; i++) {
            MockHttpServletRequest request = request("198.51.100." + i);
            request.addHeader("X-Forwarded-For", "192.0.2.55");
            limiter.checkLogin(request, "user-" + i + "@luppa.test");
        }
    }

    @Test
    void usesForwardedForOnlyFromTrustedProxy() {
        PublicEndpointRateLimiter limiter = limiter(List.of("10.0.0.0/8"));

        for (int i = 0; i < 20; i++) {
            MockHttpServletRequest request = request("10.1.1.1");
            request.addHeader("X-Forwarded-For", "192.0.2.55");
            limiter.checkLogin(request, "user-" + i + "@luppa.test");
        }

        MockHttpServletRequest blocked = request("10.1.1.1");
        blocked.addHeader("X-Forwarded-For", "192.0.2.55");
        assertThatThrownBy(() -> limiter.checkLogin(blocked, "another@luppa.test"))
                .isInstanceOf(RateLimitExceededException.class);
    }

    private PublicEndpointRateLimiter limiter(List<String> trustedProxyCidrs) {
        RateLimitProperties properties = new RateLimitProperties(true, trustedProxyCidrs);
        ClientIpResolver resolver = new ClientIpResolver(properties);
        return new PublicEndpointRateLimiter(properties, resolver);
    }

    private MockHttpServletRequest request(String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddress);
        return request;
    }
}
