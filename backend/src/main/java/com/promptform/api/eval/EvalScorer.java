package com.promptform.api.eval;

import com.promptform.api.generation.GenerationModels.UiNode;
import com.promptform.api.generation.GenerationModels.UiSchema;
import com.promptform.api.generation.SchemaValidator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class EvalScorer {
    private final SchemaValidator validator;

    public EvalScorer(SchemaValidator validator) {
        this.validator = validator;
    }

    public EvalScore score(EvalCase testCase, UiSchema schema) {
        boolean schemaValid = true;
        try {
            validator.validate(schema);
        } catch (RuntimeException exception) {
            schemaValid = false;
        }
        if (!schemaValid) return new EvalScore(false, false, BigDecimal.ZERO, false);

        Set<String> actualFields = new HashSet<>();
        collectFields(schema.children(), actualFields);
        long matchedFields = testCase.requiredFields().stream().filter(actualFields::contains).count();
        BigDecimal requirementScore = BigDecimal.valueOf(matchedFields)
                .divide(BigDecimal.valueOf(testCase.requiredFields().size()), 4, RoundingMode.HALF_UP);
        boolean passed = requirementScore.compareTo(BigDecimal.ONE) == 0;
        return new EvalScore(true, true, requirementScore, passed);
    }

    private void collectFields(Iterable<UiNode> nodes, Set<String> fields) {
        for (UiNode node : nodes) {
            if (node.name() != null && !node.name().isBlank()) fields.add(node.name());
            if (node.children() != null) collectFields(node.children(), fields);
        }
    }
}
