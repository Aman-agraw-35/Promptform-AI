package com.promptform.api.ratelimit;

import com.promptform.api.config.RateLimitProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class InMemoryRateLimiter implements RateLimiter {
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final int MAX_TRACKED_KEYS = 10_000;
    private final RateLimitProperties properties;
    private final Clock clock;
    private final Map<String, Window> windows = new HashMap<>();

    @Autowired
    public InMemoryRateLimiter(RateLimitProperties properties) {
        this(properties, Clock.systemUTC());
    }

    InMemoryRateLimiter(RateLimitProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public synchronized boolean allow(String key) {
        if (!properties.enabled()) return true;
        Instant now = clock.instant();
        Window current = windows.get(key);
        if (current == null || current.startedAt().plus(WINDOW).isBefore(now)) {
            if (windows.size() >= MAX_TRACKED_KEYS) windows.entrySet().removeIf(entry -> entry.getValue().startedAt().plus(WINDOW).isBefore(now));
            windows.put(key, new Window(now, 1));
            return true;
        }
        if (current.count() >= properties.requestsPerMinute()) return false;
        windows.put(key, new Window(current.startedAt(), current.count() + 1));
        return true;
    }

    private record Window(Instant startedAt, int count) { }
}
