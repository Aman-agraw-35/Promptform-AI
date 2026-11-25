package com.promptform.api.generation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "promptform.llm.provider", havingValue = "openrouter")
public class OpenRouterLLMProvider implements LLMProvider {
    private static final Logger logger = LoggerFactory.getLogger(OpenRouterLLMProvider.class);
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final Duration timeout;

    public OpenRouterLLMProvider(ObjectMapper objectMapper, OpenRouterProperties properties) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
        this.apiKey = properties.apiKey();
        this.model = properties.model();
        this.baseUrl = properties.baseUrl().replaceAll("/+$", "");
        this.timeout = properties.timeout();
    }

    @Override
    public String generate(String prompt) {
        return generateResponse(prompt).text();
    }

    @Override
    public LLMGenerationResponse generateResponse(String prompt) {
        if (apiKey.isBlank()) {
            throw new GenerationException("OpenRouter is enabled but OPENROUTER_API_KEY is missing.");
        }
        try {
            Instant startedAt = Instant.now();
            String body = objectMapper.writeValueAsString(new ChatRequest(model, new ChatMessage[]{
                    new ChatMessage("system", systemPrompt()),
                    new ChatMessage("user", prompt)
            }));
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/chat/completions"))
                    .timeout(timeout)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            logger.info("OpenRouter request started model={} promptChars={}", model, prompt.length());
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            logger.info("OpenRouter response received model={} status={} durationMs={}",
                    model, response.statusCode(), Duration.between(startedAt, Instant.now()).toMillis());
            if (response.statusCode() / 100 != 2) {
                logger.error("OpenRouter request failed status={} body={}", response.statusCode(), response.body());
                if (response.statusCode() == 429) {
                    throw new GenerationException("OpenRouter quota or rate limit reached.");
                }
                throw new GenerationException("OpenRouter generation failed with HTTP " + response.statusCode() + ".");
            }
            JsonNode root = objectMapper.readTree(response.body());
            String text = root.at("/choices/0/message/content").asText();
            if (text.isBlank()) throw new GenerationException("OpenRouter returned an empty result.");
            JsonNode usage = root.path("usage");
            return new LLMGenerationResponse(text,
                    usage.path("prompt_tokens").asLong(0),
                    usage.path("completion_tokens").asLong(0),
                    usage.path("total_tokens").asLong(0));
        } catch (GenerationException exception) {
            throw exception;
        } catch (Exception exception) {
            logger.error("OpenRouter request failed type={} message={}",
                    exception.getClass().getSimpleName(), exception.getMessage(), exception);
            throw new GenerationException("OpenRouter generation failed: "
                    + exception.getClass().getSimpleName() + " - " + exception.getMessage(), exception);
        }
    }

    private String systemPrompt() {
        return "Return only valid JSON for a form schema with this shape: "
                + "{title:string, description:string, children:[{type,name,label,description?,required?,options?,visibleWhen?,validation?}]}. "
                + "Allowed types are text, number, textarea, select, radio, checkboxGroup. "
                + "Select, radio, and checkboxGroup require options with value and label. "
                + "Every child requires type, name, and label. Do not return markdown or explanations.";
    }

    private record ChatRequest(String model, ChatMessage[] messages) {}
    private record ChatMessage(String role, String content) {}
}
