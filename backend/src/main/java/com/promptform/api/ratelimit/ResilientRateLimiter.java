package com.promptform.api.ratelimit;

import com.promptform.api.config.RedisProperties;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class ResilientRateLimiter implements RateLimiter {
    private static final Logger logger = LoggerFactory.getLogger(ResilientRateLimiter.class);
    private final Optional<RedisRateLimiter> redisRateLimiter;
    private final InMemoryRateLimiter fallback;
    private final RedisProperties redisProperties;

    public ResilientRateLimiter(Optional<RedisRateLimiter> redisRateLimiter,
                                InMemoryRateLimiter fallback, RedisProperties redisProperties) {
        this.redisRateLimiter = redisRateLimiter;
        this.fallback = fallback;
        this.redisProperties = redisProperties;
    }

    @Override
    public boolean allow(String key) {
        if (redisProperties.enabled() && redisRateLimiter.isPresent()) {
            try {
                return redisRateLimiter.get().allow(key);
            } catch (RuntimeException exception) {
                logger.warn("Redis rate limiting is unavailable; using in-memory fallback.");
            }
        }
        return fallback.allow(key);
    }
}
