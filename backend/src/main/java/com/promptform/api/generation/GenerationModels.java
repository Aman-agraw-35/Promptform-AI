package com.promptform.api.generation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class GenerationModels {
    private GenerationModels() {}

    public record GenerateRequest(@NotBlank @Size(max = 2000) String prompt) {}
    public record FormOption(String value, String label) {}
    public record VisibleWhen(String field, String equals, String notEquals, List<String> in) {}
    public record ValidationRules(Double min, Double max, String pattern) {}
    public record UiNode(String type, String name, String label, String description, Boolean required, Boolean unique, Object defaultValue, List<FormOption> options, List<UiNode> children, VisibleWhen visibleWhen, ValidationRules validation) {}
    public record UiSchema(String title, String description, List<UiNode> children) {}
}
