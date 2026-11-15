package com.promptform.api.eval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class EvalDatasetTest {
    @Test
    void datasetContainsTheRequiredNumberOfUniqueCases() {
        EvalDataset dataset = new EvalDataset(new ObjectMapper());

        assertTrue(dataset.cases().size() >= 30 && dataset.cases().size() <= 50);
        assertEquals(dataset.cases().size(), dataset.cases().stream().map(EvalCase::id).distinct().count());
    }
}
