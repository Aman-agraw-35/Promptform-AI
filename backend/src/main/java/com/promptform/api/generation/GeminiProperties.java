package com.promptform.api.generation;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "promptform.llm")
public class GeminiProperties {
    private String provider = "demo";
    private String geminiApiKey = "";
    private String model = "gemini-3.8-flash";
    private long timeoutSeconds = 30;
    private double costPerMillionTokens = 0;

    public String provider() { return provider; }
    public String apiKey() { return geminiApiKey; }
    public String model() { return model; }
    public Duration timeout() {
        if (timeoutSeconds < 1) throw new IllegalStateException("Gemini timeout must be at least one second.");
        return Duration.ofSeconds(timeoutSeconds);
    }
    public double costPerMillionTokens() { return costPerMillionTokens; }

    public void setProvider(String value) { provider = value; }
    public void setGeminiApiKey(String value) { geminiApiKey = value; }
    public void setModel(String value) { model = value; }
    public void setTimeoutSeconds(long value) { timeoutSeconds = value; }
    public void setCostPerMillionTokens(double value) { costPerMillionTokens = value; }
}
