package com.promptform.api.submission;

import com.fasterxml.jackson.databind.JsonNode;
import com.promptform.api.generation.GenerationModels.UiNode;
import com.promptform.api.generation.GenerationModels.UiSchema;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SubmissionValueValidator {
    public void validate(UiSchema schema, JsonNode values) {
        validateNodes(schema.children(), values);
    }

    private void validateNodes(Iterable<UiNode> nodes, JsonNode values) {
        for (UiNode node : nodes) {
            if (node.children() != null) {
                validateNodes(node.children(), values);
            }
            if (node.name() == null || !isVisible(node, values)) {
                continue;
            }

            JsonNode value = values.get(node.name());
            boolean required = Boolean.TRUE.equals(node.required());
            if (value == null || value.isNull() || (value.isTextual() && value.asText().isBlank())) {
                if (required) throw invalid(node.name(), "is required.");
                continue;
            }

            switch (node.type()) {
                case "number" -> validateNumber(node, value);
                case "checkboxGroup" -> validateCheckboxes(node, value);
                case "select", "radio" -> validateOption(node, value);
                case "text", "textarea" -> validateText(node, value);
                default -> throw invalid(node.name(), "has an unsupported type.");
            }
        }
    }

    private void validateNumber(UiNode node, JsonNode value) {
        if (!value.isNumber() || !Double.isFinite(value.asDouble())) {
            throw invalid(node.name(), "must be a valid number.");
        }
        double number = value.asDouble();
        if (node.validation() != null && node.validation().min() != null && number < node.validation().min()) {
            throw invalid(node.name(), "must be at least " + node.validation().min() + ".");
        }
        if (node.validation() != null && node.validation().max() != null && number > node.validation().max()) {
            throw invalid(node.name(), "must be at most " + node.validation().max() + ".");
        }
    }

    private void validateText(UiNode node, JsonNode value) {
        if (!value.isTextual()) throw invalid(node.name(), "must be text.");
        String text = value.asText();
        validateTextRules(node, text);
    }

    private void validateOption(UiNode node, JsonNode value) {
        if (!value.isTextual()) throw invalid(node.name(), "must be a selected option.");
        Set<String> options = optionValues(node);
        if (!options.contains(value.asText())) throw invalid(node.name(), "contains an invalid option.");
        validateTextRules(node, value.asText());
    }

    private void validateCheckboxes(UiNode node, JsonNode value) {
        if (!value.isArray()) throw invalid(node.name(), "must be an array of selected options.");
        Set<String> options = optionValues(node);
        Set<String> selected = new HashSet<>();
        for (JsonNode item : value) {
            if (!item.isTextual() || !selected.add(item.asText()) || !options.contains(item.asText())) {
                throw invalid(node.name(), "contains an invalid option.");
            }
        }
        if (Boolean.TRUE.equals(node.required()) && selected.isEmpty()) {
            throw invalid(node.name(), "requires at least one option.");
        }
    }

    private void validateTextRules(UiNode node, String value) {
        if (node.validation() == null) return;
        if (node.validation().min() != null && value.length() < node.validation().min()) {
            throw invalid(node.name(), "must contain at least " + node.validation().min().intValue() + " characters.");
        }
        if (node.validation().max() != null && value.length() > node.validation().max()) {
            throw invalid(node.name(), "must contain at most " + node.validation().max().intValue() + " characters.");
        }
        if (node.validation().pattern() != null && !Pattern.matches(node.validation().pattern(), value)) {
            throw invalid(node.name(), "has an invalid format.");
        }
    }

    private Set<String> optionValues(UiNode node) {
        Set<String> options = new HashSet<>();
        if (node.options() != null) {
            node.options().forEach(option -> options.add(option.value()));
        }
        return options;
    }

    private boolean isVisible(UiNode node, JsonNode values) {
        if (node.visibleWhen() == null) return true;
        JsonNode actual = values.get(node.visibleWhen().field());
        String actualValue = actual == null || actual.isNull() ? "" : actual.isTextual() ? actual.asText() : actual.toString();
        if (node.visibleWhen().equals() != null) return actualValue.equals(node.visibleWhen().equals());
        if (node.visibleWhen().notEquals() != null) return !actualValue.equals(node.visibleWhen().notEquals());
        return node.visibleWhen().in() != null && node.visibleWhen().in().contains(actualValue);
    }

    private SubmissionException invalid(String field, String reason) {
        return new SubmissionException("Field '" + field + "' " + reason);
    }
}
