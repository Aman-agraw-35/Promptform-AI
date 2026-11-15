package com.promptform.api.eval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class EvalDataset {
    private static final String DATASET_PATH = "evals/prompts.json";
    private final List<EvalCase> cases;

    public EvalDataset(ObjectMapper objectMapper) {
        try (InputStream input = new ClassPathResource(DATASET_PATH).getInputStream()) {
            cases = List.copyOf(objectMapper.readValue(input, new TypeReference<>() { }));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load the evaluation dataset.", exception);
        }
        validateCases(cases);
    }

    public List<EvalCase> cases() {
        return cases;
    }

    private void validateCases(List<EvalCase> cases) {
        if (cases.size() < 30 || cases.size() > 50) {
            throw new IllegalStateException("The evaluation dataset must contain 30 to 50 cases.");
        }
        if (cases.stream().anyMatch(testCase -> testCase.id() == null || testCase.id().isBlank()
                || testCase.prompt() == null || testCase.prompt().isBlank()
                || testCase.requiredFields() == null || testCase.requiredFields().isEmpty())) {
            throw new IllegalStateException("Every evaluation case needs an id, prompt, and required fields.");
        }
        long uniqueIds = cases.stream().map(EvalCase::id).distinct().count();
        if (uniqueIds != cases.size()) throw new IllegalStateException("Evaluation case ids must be unique.");
    }
}
