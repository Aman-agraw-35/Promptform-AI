package com.promptform.api.generation;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "promptform.llm")
public class OpenRouterProperties {
    private String openrouterApiKey = "";
    private String openrouterModel = "openrouter/free";
    private String openrouterBaseUrl = "https://openrouter.ai/api/v1";
    private long timeoutSeconds = 120;

    public String apiKey() { return openrouterApiKey; }
    public String model() { return openrouterModel; }
    public String baseUrl() { return openrouterBaseUrl; }

    public Duration timeout() {
        if (timeoutSeconds < 1) throw new IllegalStateException("OpenRouter timeout must be at least one second.");
        return Duration.ofSeconds(timeoutSeconds);
    }

    public void setOpenrouterApiKey(String value) { openrouterApiKey = value; }
    public void setOpenrouterModel(String value) { openrouterModel = value; }
    public void setOpenrouterBaseUrl(String value) { openrouterBaseUrl = value; }
    public void setTimeoutSeconds(long value) { timeoutSeconds = value; }
}
