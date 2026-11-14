package com.promptform.api.generation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class GenerationModels {
    private GenerationModels() {}

    public record GenerateRequest(@NotBlank @Size(max = 2000) String prompt) {}
    public record FormOption(String value, String label) {}
    public record UiNode(String type, String name, String label, String description, Boolean required, List<FormOption> options, List<UiNode> children) {}
    public record UiSchema(String title, String description, List<UiNode> children) {}
}
