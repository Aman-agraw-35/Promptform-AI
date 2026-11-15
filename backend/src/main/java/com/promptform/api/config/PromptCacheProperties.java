package com.promptform.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "promptform.prompt-cache")
public record PromptCacheProperties(boolean enabled, int maxEntries, long ttlSeconds) {
    public PromptCacheProperties {
        if (maxEntries < 1) throw new IllegalArgumentException("Prompt cache must allow at least one entry.");
        if (ttlSeconds < 1) throw new IllegalArgumentException("Prompt cache TTL must be at least one second.");
    }
}
