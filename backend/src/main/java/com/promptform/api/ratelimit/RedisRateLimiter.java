package com.promptform.api.ratelimit;

import com.promptform.api.config.RateLimitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "promptform.redis", name = "enabled", havingValue = "true")
public class RedisRateLimiter implements RateLimiter {
    private final StringRedisTemplate redis;
    private final RateLimitProperties properties;

    public RedisRateLimiter(StringRedisTemplate redis, RateLimitProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    @Override
    public boolean allow(String key) {
        String redisKey = "promptform:rate-limit:" + key;
        Long count = redis.opsForValue().increment(redisKey);
        if (count != null && count == 1) redis.expire(redisKey, java.time.Duration.ofMinutes(1));
        return count == null || count <= properties.requestsPerMinute();
    }
}
