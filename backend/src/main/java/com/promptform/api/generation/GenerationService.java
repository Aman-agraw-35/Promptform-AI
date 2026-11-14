package com.promptform.api.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptform.api.generation.GenerationModels.UiSchema;
import org.springframework.stereotype.Service;

@Service
public class GenerationService {
    private static final int MAX_REPAIR_ATTEMPTS = 2;
    private static final int MAX_REPAIR_RESPONSE_LENGTH = 8_000;
    private final LLMProvider provider;
    private final ObjectMapper objectMapper;
    private final SchemaValidator validator;

    public GenerationService(LLMProvider provider, ObjectMapper objectMapper, SchemaValidator validator) {
        this.provider = provider;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public UiSchema generate(String prompt) {
        String currentPrompt = prompt;
        String lastResponse = "";
        for (int attempt = 0; attempt <= MAX_REPAIR_ATTEMPTS; attempt++) {
            lastResponse = provider.generate(currentPrompt);
            try {
                UiSchema schema = parseAndValidate(lastResponse);
                return schema;
            } catch (GenerationException exception) {
                if (attempt == MAX_REPAIR_ATTEMPTS) throw new GenerationException("The model could not produce a valid form after repair attempts.", exception);
                currentPrompt = repairPrompt(prompt, lastResponse, exception.getMessage());
            }
        }
        throw new GenerationException("The model could not produce a valid form.");
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
}
