package com.promptform.api.generation;

import com.promptform.api.generation.GenerationModels.UiNode;
import com.promptform.api.generation.GenerationModels.UiSchema;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class SchemaValidator {
    private static final Set<String> FIELD_TYPES = Set.of("text", "number", "select", "radio", "checkboxGroup", "textarea");

    public void validate(UiSchema schema) {
        if (schema == null || blank(schema.title())) throw new GenerationException("The model returned a form without a title.");
        if (schema.children() == null || schema.children().isEmpty()) throw new GenerationException("The model returned a form without any fields.");
        Set<String> names = new HashSet<>();
        schema.children().forEach(node -> validateNode(node, names, 0));
    }

    private void validateNode(UiNode node, Set<String> names, int depth) {
        if (node == null || depth > 8 || blank(node.type()) || blank(node.label())) throw new GenerationException("The form schema contains an invalid node.");
        if (node.type().equals("section")) {
            if (node.children() == null || node.children().isEmpty()) throw new GenerationException("A form section must contain fields.");
            node.children().forEach(child -> validateNode(child, names, depth + 1));
            return;
        }
        if (!FIELD_TYPES.contains(node.type()) || blank(node.name()) || !names.add(node.name())) throw new GenerationException("The form schema contains an unsupported or duplicate field.");
        if ((node.type().equals("select") || node.type().equals("radio") || node.type().equals("checkboxGroup")) && (node.options() == null || node.options().isEmpty())) throw new GenerationException("Option fields must contain choices.");
        if (node.options() != null && node.options().stream().anyMatch(option -> option == null || blank(option.value()) || blank(option.label()))) throw new GenerationException("Every option needs a value and label.");
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
}
