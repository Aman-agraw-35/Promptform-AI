package com.promptform.api.eval;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/evals")
public class EvalController {
    private final EvalService evalService;

    public EvalController(EvalService evalService) {
        this.evalService = evalService;
    }

    @PostMapping("/run")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public EvalModels.RunResponse startRun() {
        return evalService.startRun();
    }

    @GetMapping("/runs")
    public List<EvalModels.RunResponse> listRuns() {
        return evalService.listRuns();
    }

    @GetMapping("/runs/{runId}")
    public EvalModels.RunDetails getRun(@PathVariable UUID runId) {
        return evalService.getRun(runId);
    }
}
