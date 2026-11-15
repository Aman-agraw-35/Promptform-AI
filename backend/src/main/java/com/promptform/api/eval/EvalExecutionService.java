package com.promptform.api.eval;

import com.promptform.api.generation.GenerationModels.UiSchema;
import com.promptform.api.generation.GenerationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.scheduling.annotation.Async;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EvalExecutionService {
    private static final int MAX_ERROR_LENGTH = 500;
    private static final Logger logger = LoggerFactory.getLogger(EvalExecutionService.class);
    private final EvalRunRepository runRepository;
    private final EvalResultRepository resultRepository;
    private final GenerationService generationService;
    private final EvalScorer scorer;

    public EvalExecutionService(EvalRunRepository runRepository, EvalResultRepository resultRepository,
                                GenerationService generationService, EvalScorer scorer) {
        this.runRepository = runRepository;
        this.resultRepository = resultRepository;
        this.generationService = generationService;
        this.scorer = scorer;
    }

    @Async("evalExecutor")
    public void execute(UUID runId, List<EvalCase> cases) {
        EvalRunEntity run = runRepository.findById(runId)
                .orElseThrow(() -> new EvalException("Evaluation run was not found."));
        try {
            executeCases(runId, cases, run);
        } catch (RuntimeException exception) {
            run.fail(shorten(exception.getMessage()), Instant.now());
            runRepository.save(run);
            logger.error("Evaluation run {} failed unexpectedly", runId, exception);
        }
    }

    private void executeCases(UUID runId, List<EvalCase> cases, EvalRunEntity run) {
        int passedCases = 0;
        for (EvalCase testCase : cases) {
            long startedAt = System.nanoTime();
            EvalScore score;
            String errorMessage = null;
            try {
                UiSchema schema = generationService.generate(testCase.prompt());
                score = scorer.score(testCase, schema);
            } catch (RuntimeException exception) {
                score = new EvalScore(false, false, BigDecimal.ZERO, false);
                errorMessage = shorten(exception.getMessage());
            }
            if (score.passed()) passedCases++;
            resultRepository.save(new EvalResultEntity(
                    UUID.randomUUID(), runId, testCase, score.schemaValid(), score.rendersWithoutErrors(),
                    score.requirementScore(), score.passed(), elapsedMillis(startedAt), errorMessage, Instant.now()));
        }

        BigDecimal passRate = cases.isEmpty() ? BigDecimal.ZERO : BigDecimal.valueOf(passedCases)
                .divide(BigDecimal.valueOf(cases.size()), 4, java.math.RoundingMode.HALF_UP);
        run.complete(passedCases, passRate, Instant.now());
        runRepository.save(run);
    }

    private long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private String shorten(String message) {
        if (message == null || message.isBlank()) return "Generation failed.";
        return message.substring(0, Math.min(MAX_ERROR_LENGTH, message.length()));
    }
}
