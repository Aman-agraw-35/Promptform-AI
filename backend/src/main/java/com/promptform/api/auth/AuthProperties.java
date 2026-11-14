package com.promptform.api.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "promptform.auth")
public record AuthProperties(String jwtSecret, long accessTokenMinutes) {
}
