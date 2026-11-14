package com.promptform.api.generation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
@ConditionalOnProperty(name = "promptform.llm.provider", havingValue = "gemini")
public class GeminiLLMProvider implements LLMProvider {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;
    private final Duration timeout;

    public GeminiLLMProvider(ObjectMapper objectMapper, GeminiProperties properties) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
        this.apiKey = properties.apiKey();
        this.model = properties.model();
        this.timeout = properties.timeout();
    }

    @Override
    public String generate(String prompt) {
        if (apiKey.isBlank()) throw new GenerationException("Gemini is enabled but GEMINI_API_KEY is missing.");
        try {
            String body = objectMapper.writeValueAsString(new GeminiRequest(new GeminiContent[]{new GeminiContent(new GeminiPart[]{new GeminiPart(systemPrompt(prompt))})}, new GenerationConfig("application/json", responseSchema())));
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent")).timeout(timeout).header("Content-Type", "application/json").header("x-goog-api-key", apiKey).POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) throw new GenerationException("Gemini returned HTTP " + response.statusCode() + ": " + response.body());
            JsonNode root = objectMapper.readTree(response.body());
            String text = root.at("/candidates/0/content/parts/0/text").asText();
            if (text.isBlank()) throw new GenerationException("Gemini returned an empty result.");
            return text;
        } catch (GenerationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new GenerationException("Gemini generation failed.", exception);
        }
    }

    private String systemPrompt(String prompt) {
        return "Generate a form schema matching the response schema exactly. Return at least one child field. Supported node types are text, number, textarea, select, radio, and checkboxGroup. Every field needs a unique name and label. Option fields require options with value and label. Use visibleWhen with a field and equals when a field is conditional. Use validation for min, max, or pattern rules. Do not include HTML, code, markdown, explanations, or an outer wrapper. User request: " + prompt;
    }

    private JsonNode responseSchema() throws Exception {
        return objectMapper.readTree("""
            {
              "type": "object",
              "properties": {
                "title": {"type": "string"},
                "description": {"type": "string"},
                "children": {
                  "type": "array",
                  "minItems": 1,
                  "items": {
                    "type": "object",
                    "properties": {
                      "type": {"type": "string", "enum": ["text", "number", "textarea", "select", "radio", "checkboxGroup"]},
                      "name": {"type": "string"},
                      "label": {"type": "string"},
                      "description": {"type": "string"},
                      "required": {"type": "boolean"},
                      "defaultValue": {"type": ["string", "number", "boolean", "null"]},
                      "visibleWhen": {
                        "type": "object",
                        "properties": {
                          "field": {"type": "string"},
                          "equals": {"type": "string"},
                          "notEquals": {"type": "string"},
                          "in": {"type": "array", "items": {"type": "string"}}
                        },
                        "required": ["field"]
                      },
                      "validation": {
                        "type": "object",
                        "properties": {
                          "min": {"type": "number"},
                          "max": {"type": "number"},
                          "pattern": {"type": "string"}
                        }
                      },
                      "options": {
                        "type": "array",
                        "items": {
                          "type": "object",
                          "properties": {"value": {"type": "string"}, "label": {"type": "string"}},
                          "required": ["value", "label"]
                        }
                      }
                    },
                    "required": ["type", "name", "label"]
                  }
                }
              },
              "required": ["title", "children"]
            }
            """);
    }

    private record GeminiRequest(GeminiContent[] contents, GenerationConfig generationConfig) {}
    private record GeminiContent(GeminiPart[] parts) {}
    private record GeminiPart(String text) {}
    private record GenerationConfig(String responseMimeType, JsonNode responseSchema) {}
}
