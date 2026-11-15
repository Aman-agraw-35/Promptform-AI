package com.promptform.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "promptform.redis")
public record RedisProperties(boolean enabled) {
}
