package com.promptform.api.generation;

public interface LLMProvider {
    String generate(String prompt);
}
