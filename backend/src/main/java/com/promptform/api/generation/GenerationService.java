package com.promptform.api.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptform.api.generation.GenerationModels.UiSchema;
import org.springframework.stereotype.Service;

@Service
public class GenerationService {
    private final LLMProvider provider;
    private final ObjectMapper objectMapper;
    private final SchemaValidator validator;

    public GenerationService(LLMProvider provider, ObjectMapper objectMapper, SchemaValidator validator) {
        this.provider = provider;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public UiSchema generate(String prompt) {
        try {
            UiSchema schema = objectMapper.readValue(extractJson(provider.generate(prompt)), UiSchema.class);
            validator.validate(schema);
            return schema;
        } catch (GenerationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new GenerationException("The model returned invalid JSON.", exception);
        }
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
}
