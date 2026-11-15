package com.promptform.api.eval;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class EvalService {
    private final EvalDataset dataset;
    private final EvalRunRepository runRepository;
    private final EvalResultRepository resultRepository;
    private final EvalExecutionService executionService;

    public EvalService(EvalDataset dataset, EvalRunRepository runRepository,
                       EvalResultRepository resultRepository, EvalExecutionService executionService) {
        this.dataset = dataset;
        this.runRepository = runRepository;
        this.resultRepository = resultRepository;
        this.executionService = executionService;
    }

    public EvalModels.RunResponse startRun() {
        List<EvalCase> cases = dataset.cases();
        EvalRunEntity run = runRepository.save(new EvalRunEntity(UUID.randomUUID(), cases.size(), Instant.now()));
        executionService.execute(run.getId(), cases);
        return toResponse(run);
    }

    public List<EvalModels.RunResponse> listRuns() {
        return runRepository.findTop20ByOrderByStartedAtDesc().stream().map(this::toResponse).toList();
    }

    public EvalModels.RunDetails getRun(UUID runId) {
        EvalRunEntity run = runRepository.findById(runId)
                .orElseThrow(() -> new EvalException("Evaluation run was not found."));
        List<EvalModels.ResultResponse> results = resultRepository.findAllByRunIdOrderByCreatedAtAsc(runId).stream()
                .map(this::toResultResponse)
                .toList();
        return new EvalModels.RunDetails(toResponse(run), results);
    }

    private EvalModels.RunResponse toResponse(EvalRunEntity run) {
        return new EvalModels.RunResponse(run.getId(), run.getStatus(), run.getTotalCases(),
                run.getPassedCases(), run.getPassRate(), run.getStartedAt(), run.getCompletedAt());
    }

    private EvalModels.ResultResponse toResultResponse(EvalResultEntity result) {
        return new EvalModels.ResultResponse(result.getId(), result.getCaseId(), result.isSchemaValid(),
                result.isRendersWithoutErrors(), result.getRequirementScore(), result.isPassed(),
                result.getLatencyMs(), result.getErrorMessage());
    }
}
