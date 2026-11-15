package com.promptform.api.eval;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "eval_results")
public class EvalResultEntity {
    @Id
    private UUID id;

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "case_id", nullable = false, length = 80)
    private String caseId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @Column(name = "schema_valid", nullable = false)
    private boolean schemaValid;

    @Column(name = "renders_without_errors", nullable = false)
    private boolean rendersWithoutErrors;

    @Column(name = "requirement_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal requirementScore;

    @Column(nullable = false)
    private boolean passed;

    @Column(name = "latency_ms", nullable = false)
    private long latencyMs;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EvalResultEntity() { }

    public EvalResultEntity(UUID id, UUID runId, EvalCase testCase, boolean schemaValid,
                            boolean rendersWithoutErrors, BigDecimal requirementScore,
                            boolean passed, long latencyMs, String errorMessage, Instant createdAt) {
        this.id = id;
        this.runId = runId;
        this.caseId = testCase.id();
        this.prompt = testCase.prompt();
        this.schemaValid = schemaValid;
        this.rendersWithoutErrors = rendersWithoutErrors;
        this.requirementScore = requirementScore;
        this.passed = passed;
        this.latencyMs = latencyMs;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getRunId() { return runId; }
    public String getCaseId() { return caseId; }
    public String getPrompt() { return prompt; }
    public boolean isSchemaValid() { return schemaValid; }
    public boolean isRendersWithoutErrors() { return rendersWithoutErrors; }
    public BigDecimal getRequirementScore() { return requirementScore; }
    public boolean isPassed() { return passed; }
    public long getLatencyMs() { return latencyMs; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
}
