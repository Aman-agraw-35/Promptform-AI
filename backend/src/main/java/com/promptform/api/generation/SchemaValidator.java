package com.promptform.api.generation;

import com.promptform.api.generation.GenerationModels.UiNode;
import com.promptform.api.generation.GenerationModels.UiSchema;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Component
public class SchemaValidator {
    private static final Set<String> FIELD_TYPES = Set.of("text", "number", "select", "radio", "checkboxGroup", "textarea");
    private static final Set<String> LAYOUT_TYPES = Set.of("section", "columns");
    private static final int MAX_FIELDS = 50;
    private static final int MAX_DEPTH = 8;

    public void validate(UiSchema schema) {
        if (schema == null || blank(schema.title())) throw new GenerationException("The model returned a form without a title.");
        if (schema.children() == null || schema.children().isEmpty()) throw new GenerationException("The model returned a form without any fields.");
        Set<String> names = new HashSet<>();
        Map<String, String> conditions = new HashMap<>();
        int fieldCount = countFields(schema.children(), 0);
        if (fieldCount > MAX_FIELDS) throw new GenerationException("The form schema contains too many fields.");
        collectNames(schema.children(), names, conditions, 0);
        names.clear();
        schema.children().forEach(node -> validateNode(node, names, 0));
        validateConditions(conditions, names);
    }

    private void validateNode(UiNode node, Set<String> names, int depth) {
        if (node == null || depth > MAX_DEPTH || blank(node.type()) || blank(node.label())) throw new GenerationException("The form schema contains an invalid node.");
        if (LAYOUT_TYPES.contains(node.type())) {
            if (node.children() == null || node.children().isEmpty()) throw new GenerationException("A form section must contain fields.");
            node.children().forEach(child -> validateNode(child, names, depth + 1));
            return;
        }
        if (!FIELD_TYPES.contains(node.type()) || blank(node.name()) || !names.add(node.name())) throw new GenerationException("The form schema contains an unsupported or duplicate field.");
        if ((node.type().equals("select") || node.type().equals("radio") || node.type().equals("checkboxGroup")) && (node.options() == null || node.options().isEmpty())) throw new GenerationException("Option fields must contain choices.");
        if (node.options() != null && node.options().stream().anyMatch(option -> option == null || blank(option.value()) || blank(option.label()))) throw new GenerationException("Every option needs a value and label.");
        if (node.options() != null && node.options().stream().map(option -> option.value()).distinct().count() != node.options().size()) throw new GenerationException("Option values must be unique.");
        if (node.validation() != null) {
            if (node.validation().min() != null && node.validation().max() != null && node.validation().min() > node.validation().max()) throw new GenerationException("A field minimum cannot be greater than its maximum.");
            if (node.validation().pattern() != null) {
                try { Pattern.compile(node.validation().pattern()); } catch (PatternSyntaxException exception) { throw new GenerationException("A field contains an invalid pattern.", exception); }
            }
        }
        if (node.defaultValue() != null && node.options() != null && node.options().stream().noneMatch(option -> option.value().equals(String.valueOf(node.defaultValue())))) throw new GenerationException("A default value must match one of the field options.");
    }

    private int countFields(Iterable<UiNode> nodes, int depth) {
        int count = 0;
        for (UiNode node : nodes) {
            if (node == null || depth > MAX_DEPTH) throw new GenerationException("The form schema is nested too deeply.");
            if (FIELD_TYPES.contains(node.type())) count++;
            if (node.children() != null) count += countFields(node.children(), depth + 1);
        }
        return count;
    }

    private void collectNames(Iterable<UiNode> nodes, Set<String> names, Map<String, String> conditions, int depth) {
        for (UiNode node : nodes) {
            if (node == null || depth > MAX_DEPTH) throw new GenerationException("The form schema is nested too deeply.");
            if (FIELD_TYPES.contains(node.type())) {
                if (blank(node.name()) || !names.add(node.name())) throw new GenerationException("The form schema contains an unsupported or duplicate field.");
                if (node.visibleWhen() != null) conditions.put(node.name(), node.visibleWhen().field());
            }
            if (node.children() != null) collectNames(node.children(), names, conditions, depth + 1);
        }
    }

    private void validateConditions(Map<String, String> conditions, Set<String> names) {
        conditions.forEach((field, dependency) -> {
            if (blank(dependency) || !names.contains(dependency)) throw new GenerationException("A visibility condition references a missing field.");
            if (field.equals(dependency)) throw new GenerationException("A field cannot depend on its own visibility.");
            if (hasCycle(field, dependency, conditions, new HashSet<>())) throw new GenerationException("Visibility conditions cannot be circular.");
        });
    }

    private boolean hasCycle(String field, String dependency, Map<String, String> conditions, Set<String> visited) {
        if (field.equals(dependency)) return true;
        if (!visited.add(dependency)) return false;
        String next = conditions.get(dependency);
        return next != null && hasCycle(field, next, conditions, visited);
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
}
