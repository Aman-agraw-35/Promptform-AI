package com.promptform.api.eval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.promptform.api.generation.GenerationModels.UiNode;
import com.promptform.api.generation.GenerationModels.UiSchema;
import com.promptform.api.generation.SchemaValidator;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class EvalScorerTest {
    private final EvalScorer scorer = new EvalScorer(new SchemaValidator());

    @Test
    void scoresRequiredFieldsAndRenderSafetySeparately() {
        EvalCase testCase = new EvalCase("test", "test prompt", List.of("name", "email"));
        UiSchema schema = new UiSchema("Test", null, List.of(
                new UiNode("text", "name", "Name", null, true, null, null, null, null, null)));

        EvalScore score = scorer.score(testCase, schema);

        assertTrue(score.schemaValid());
        assertTrue(score.rendersWithoutErrors());
        assertEquals(new BigDecimal("0.5000"), score.requirementScore());
        assertFalse(score.passed());
    }
}
