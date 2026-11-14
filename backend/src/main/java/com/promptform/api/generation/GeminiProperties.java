package com.promptform.api.generation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "promptform.llm")
public class GeminiProperties {
    private String provider = "demo";
    private String geminiApiKey = "";
    private String model = "gemini-3.5-flash";
    private long timeoutSeconds = 30;
    public String provider() { return provider; }
    public String apiKey() { return geminiApiKey; }
    public String model() { return model; }
    public Duration timeout() { return Duration.ofSeconds(timeoutSeconds); }
    public void setProvider(String provider) { this.provider = provider; }
    public void setGeminiApiKey(String value) { this.geminiApiKey = value; }
    public void setModel(String value) { this.model = value; }
    public void setTimeoutSeconds(long value) { this.timeoutSeconds = value; }
}
