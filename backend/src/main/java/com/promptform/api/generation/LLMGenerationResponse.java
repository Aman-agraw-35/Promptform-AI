package com.promptform.api.generation;

public record LLMGenerationResponse(String text, long promptTokens, long candidateTokens, long totalTokens) {
}
