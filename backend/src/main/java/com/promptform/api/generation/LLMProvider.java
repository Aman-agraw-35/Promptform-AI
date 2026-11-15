package com.promptform.api.generation;

public interface LLMProvider {
    String generate(String prompt);

    default LLMGenerationResponse generateResponse(String prompt) {
        return new LLMGenerationResponse(generate(prompt), 0, 0, 0);
    }
}
