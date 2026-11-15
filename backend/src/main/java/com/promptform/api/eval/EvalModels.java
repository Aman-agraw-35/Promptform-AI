package com.promptform.api.eval;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class EvalModels {
    private EvalModels() { }

    public record RunResponse(UUID id, String status, int totalCases, int passedCases,
                              BigDecimal passRate, Instant startedAt, Instant completedAt) { }

    public record RunDetails(RunResponse run, List<ResultResponse> results) { }

    public record ResultResponse(UUID id, String caseId, boolean schemaValid,
                                 boolean rendersWithoutErrors, BigDecimal requirementScore,
                                 boolean passed, long latencyMs, String errorMessage) { }
}
