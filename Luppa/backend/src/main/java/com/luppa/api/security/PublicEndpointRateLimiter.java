package com.luppa.api.security;

import com.luppa.api.config.RateLimitProperties;
import com.luppa.api.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PublicEndpointRateLimiter {
    private final RateLimitProperties properties;
    private final ClientIpResolver clientIpResolver;
    private final Clock clock;
    private final Map<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();

    @Autowired
    public PublicEndpointRateLimiter(RateLimitProperties properties, ClientIpResolver clientIpResolver) {
        this(properties, clientIpResolver, Clock.systemUTC());
    }

    PublicEndpointRateLimiter(RateLimitProperties properties, ClientIpResolver clientIpResolver, Clock clock) {
        this.properties = properties;
        this.clientIpResolver = clientIpResolver;
        this.clock = clock;
    }

    public void checkLogin(HttpServletRequest request, String email) {
        check(request, "login", email, Limit.of(20, Duration.ofMinutes(10)), Limit.of(8, Duration.ofMinutes(10)));
    }

    public void checkPasswordRecovery(HttpServletRequest request, String email) {
        check(request, "password-recovery", email, Limit.of(10, Duration.ofMinutes(15)), Limit.of(3, Duration.ofMinutes(15)));
    }

    public void checkPasswordReset(HttpServletRequest request) {
        checkIp(request, "password-reset", Limit.of(15, Duration.ofMinutes(15)));
    }

    public void checkVerificationRequest(HttpServletRequest request, String email) {
        check(request, "verification-request", email, Limit.of(10, Duration.ofMinutes(15)), Limit.of(3, Duration.ofMinutes(15)));
    }

    public void checkVerificationAttempt(HttpServletRequest request, String email) {
        check(request, "verification-attempt", email, Limit.of(30, Duration.ofMinutes(10)), Limit.of(10, Duration.ofMinutes(10)));
    }

    public void checkPublicSupport(HttpServletRequest request, String email) {
        check(request, "support-contact", email, Limit.of(5, Duration.ofMinutes(10)), Limit.of(3, Duration.ofHours(1)));
    }

    private void check(HttpServletRequest request, String endpoint, String email, Limit ipLimit, Limit accountLimit) {
        if (!properties.enabled()) {
            return;
        }
        checkIp(request, endpoint, ipLimit);
        String normalizedEmail = normalize(email);
        if (!normalizedEmail.isBlank()) {
            acquire("account:" + endpoint + ":" + normalizedEmail, accountLimit);
        }
    }

    private void checkIp(HttpServletRequest request, String endpoint, Limit limit) {
        if (!properties.enabled()) {
            return;
        }
        acquire("ip:" + endpoint + ":" + clientIpResolver.resolve(request), limit);
    }

    private void acquire(String key, Limit limit) {
        Instant now = Instant.now(clock);
        Deque<Instant> bucket = attempts.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (bucket) {
            Instant cutoff = now.minus(limit.window());
            while (!bucket.isEmpty() && bucket.peekFirst().isBefore(cutoff)) {
                bucket.removeFirst();
            }
            if (bucket.size() >= limit.maxAttempts()) {
                throw new RateLimitExceededException();
            }
            bucket.addLast(now);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record Limit(int maxAttempts, Duration window) {
        static Limit of(int maxAttempts, Duration window) {
            return new Limit(maxAttempts, window);
        }
    }
}
