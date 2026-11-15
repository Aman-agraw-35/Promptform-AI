package com.promptform.api.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptform.api.config.PromptCacheProperties;
import com.promptform.api.generation.GenerationModels.UiSchema;
import com.promptform.api.observability.GenerationMetrics;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GenerationService {
    private static final Logger logger = LoggerFactory.getLogger(GenerationService.class);
    private static final int MAX_REPAIR_ATTEMPTS = 2;
    private static final int MAX_REPAIR_RESPONSE_LENGTH = 8_000;
    private final LLMProvider provider;
    private final ObjectMapper objectMapper;
    private final SchemaValidator validator;
    private final GenerationMetrics metrics;
    private final PromptCacheProperties cacheProperties;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public GenerationService(LLMProvider provider, ObjectMapper objectMapper, SchemaValidator validator,
                             GenerationMetrics metrics, PromptCacheProperties cacheProperties) {
        this.provider = provider;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.metrics = metrics;
        this.cacheProperties = cacheProperties;
    }

    public UiSchema generate(String prompt) {
        String normalizedPrompt = normalizePrompt(prompt);
        logger.info("Generation service started provider={} promptChars={}", provider.getClass().getSimpleName(), normalizedPrompt.length());
        UiSchema cachedSchema = getCached(normalizedPrompt);
        if (cachedSchema != null) {
            logger.info("Returning cached form schema promptChars={}", normalizedPrompt.length());
            return cachedSchema;
        }
        Timer.Sample sample = metrics.start();
        String currentPrompt = normalizedPrompt;
        String lastResponse = "";
        String lastIssue = "unknown validation error";
        int repairAttempts = 0;
        long totalTokens = 0;
        try {
            for (int attempt = 0; attempt <= MAX_REPAIR_ATTEMPTS; attempt++) {
                logger.info("Calling model attempt={} repairAttempt={}", attempt + 1, repairAttempts);
                LLMGenerationResponse response = provider.generateResponse(currentPrompt);
                lastResponse = response.text();
                totalTokens += response.totalTokens();
                logger.info("Model response received chars={} tokens={}", lastResponse == null ? 0 : lastResponse.length(), response.totalTokens());
                try {
                    UiSchema schema = parseAndValidate(lastResponse);
                    logger.info("Model response parsed and validated fields={}", schema.children().size());
                    metrics.recordTokenUsage(totalTokens);
                    metrics.recordSuccess(sample, repairAttempts);
                    cache(normalizedPrompt, schema);
                    return schema;
                } catch (GenerationException exception) {
                    lastIssue = exception.getMessage() == null ? lastIssue : exception.getMessage();
                    logger.warn("Model response rejected attempt={} reason={}", attempt + 1, lastIssue);
                    if (attempt == MAX_REPAIR_ATTEMPTS) {
                        throw new GenerationException("The model could not produce a valid form: " + lastIssue, exception);
                    }
                    repairAttempts++;
                    currentPrompt = repairPrompt(normalizedPrompt, lastResponse, exception.getMessage());
                }
            }
        } catch (RuntimeException exception) {
            metrics.recordFailure(sample, repairAttempts);
            throw exception;
        }
        throw new GenerationException("The model could not produce a valid form.");
    }

    private String normalizePrompt(String prompt) {
        if (prompt == null) throw new GenerationException("A prompt is required.");
        String normalized = prompt.strip();
        if (normalized.isEmpty()) throw new GenerationException("A prompt is required.");
        if (normalized.indexOf('\u0000') >= 0) throw new GenerationException("The prompt contains an unsupported character.");
        return normalized;
    }

    private UiSchema getCached(String prompt) {
        if (!cacheProperties.enabled()) return null;
        CacheEntry entry = cache.get(prompt);
        if (entry == null) return null;
        if (entry.createdAt().plus(Duration.ofSeconds(cacheProperties.ttlSeconds())).isBefore(Instant.now())) {
            cache.remove(prompt, entry);
            return null;
        }
        return entry.schema();
    }

    private void cache(String prompt, UiSchema schema) {
        if (!cacheProperties.enabled()) return;
        if (cache.size() >= cacheProperties.maxEntries()) cache.clear();
        cache.put(prompt, new CacheEntry(Instant.now(), schema));
    }

    private UiSchema parseAndValidate(String response) {
        try {
            UiSchema schema = objectMapper.readValue(extractJson(response), UiSchema.class);
            validator.validate(schema);
            return schema;
        } catch (GenerationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new GenerationException("The model returned invalid JSON.", exception);
        }
    }

    private String repairPrompt(String originalPrompt, String previousResponse, String issue) {
        String boundedResponse = previousResponse == null ? "" : previousResponse.substring(0, Math.min(previousResponse.length(), MAX_REPAIR_RESPONSE_LENGTH));
        return "Repair the form schema for this original request: " + originalPrompt
                + "\nThe previous response was invalid. Return only a corrected JSON form schema."
                + "\nValidation issue: " + issue
                + "\nPrevious response:\n" + boundedResponse;
    }

    private String extractJson(String response) {
        if (response == null || response.isBlank()) throw new GenerationException("The model returned an empty result.");
        String normalized = response.trim();
        if (normalized.startsWith("```")) {
            int firstLineEnd = normalized.indexOf('\n');
            int closingFence = normalized.lastIndexOf("```");
            if (firstLineEnd > 0 && closingFence > firstLineEnd) normalized = normalized.substring(firstLineEnd + 1, closingFence).trim();
        }
        int objectStart = normalized.indexOf('{');
        int objectEnd = normalized.lastIndexOf('}');
        if (objectStart < 0 || objectEnd <= objectStart) throw new GenerationException("The model returned invalid JSON.");
        return normalized.substring(objectStart, objectEnd + 1);
    }

    private record CacheEntry(Instant createdAt, UiSchema schema) {}
}
