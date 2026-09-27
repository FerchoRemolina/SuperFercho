package com.superfercho.identity.infrastructure.mail;

import com.superfercho.identity.application.port.PasswordRecoveryAbuseGuard;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-process per-key sliding window. Sufficient without gateway rate-limit infrastructure.
 */
public final class InMemoryPasswordRecoveryAbuseGuard implements PasswordRecoveryAbuseGuard {

    private final Clock clock;
    private final int maxRequests;
    private final Duration window;
    private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();

    public InMemoryPasswordRecoveryAbuseGuard(Clock clock, int maxRequests, Duration window) {
        this.clock = clock;
        this.maxRequests = maxRequests;
        this.window = window;
    }

    @Override
    public boolean allowRequest(String clientKey) {
        Instant now = clock.instant();
        Instant cutoff = now.minus(window);
        Deque<Instant> queue = hits.computeIfAbsent(clientKey, key -> new ArrayDeque<>());
        synchronized (queue) {
            while (!queue.isEmpty() && queue.peekFirst().isBefore(cutoff)) {
                queue.removeFirst();
            }
            if (queue.size() >= maxRequests) {
                return false;
            }
            queue.addLast(now);
            return true;
        }
    }
}
