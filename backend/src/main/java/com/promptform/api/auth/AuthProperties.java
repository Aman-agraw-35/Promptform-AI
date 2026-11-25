package com.promptform.api.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "promptform.auth")
public record AuthProperties(String jwtSecret, long accessTokenMinutes, boolean secureCookie) {
    public AuthProperties {
        if (accessTokenMinutes < 1) {
            throw new IllegalArgumentException("JWT access token lifetime must be at least one minute.");
        }
    }
}
