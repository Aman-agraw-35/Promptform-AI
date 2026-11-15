package com.promptform.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "promptform.rate-limit")
public record RateLimitProperties(boolean enabled, int requestsPerMinute) {
    public RateLimitProperties {
        if (requestsPerMinute < 1) throw new IllegalArgumentException("Rate limit must allow at least one request per minute.");
    }
}
