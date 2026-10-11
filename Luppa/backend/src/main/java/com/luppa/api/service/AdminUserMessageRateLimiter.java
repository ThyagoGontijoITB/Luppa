package com.luppa.api.service;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AdminUserMessageRateLimiter {
    private static final int MAX_MESSAGES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(10);
    private static final Duration DUPLICATE_WINDOW = Duration.ofSeconds(30);

    private final Clock clock;
    private final Map<Integer, Deque<Attempt>> attemptsByAdmin = new ConcurrentHashMap<>();

    public AdminUserMessageRateLimiter() {
        this(Clock.systemDefaultZone());
    }

    AdminUserMessageRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public boolean tryAcquire(Integer adminId, Integer targetUserId, String subject, String message) {
        Instant now = Instant.now(clock);
        Deque<Attempt> attempts = attemptsByAdmin.computeIfAbsent(adminId, ignored -> new ArrayDeque<>());
        synchronized (attempts) {
            while (!attempts.isEmpty() && attempts.peekFirst().sentAt().isBefore(now.minus(WINDOW))) {
                attempts.removeFirst();
            }
            if (attempts.size() >= MAX_MESSAGES) {
                return false;
            }
            String fingerprint = fingerprint(targetUserId, subject, message);
            boolean duplicate = attempts.stream()
                    .anyMatch(attempt -> attempt.fingerprint().equals(fingerprint)
                            && !attempt.sentAt().isBefore(now.minus(DUPLICATE_WINDOW)));
            if (duplicate) {
                return false;
            }
            attempts.addLast(new Attempt(now, fingerprint));
            return true;
        }
    }

    private String fingerprint(Integer targetUserId, String subject, String message) {
        return targetUserId + "|" + normalized(subject) + "|" + normalized(message);
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim();
    }

    private record Attempt(Instant sentAt, String fingerprint) {
    }
}
