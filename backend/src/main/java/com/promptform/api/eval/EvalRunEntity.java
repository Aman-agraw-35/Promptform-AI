package com.promptform.api.eval;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "eval_runs")
public class EvalRunEntity {
    @Id
    private UUID id;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "total_cases", nullable = false)
    private int totalCases;

    @Column(name = "passed_cases", nullable = false)
    private int passedCases;

    @Column(name = "pass_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal passRate;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failure_message")
    private String failureMessage;

    protected EvalRunEntity() { }

    public EvalRunEntity(UUID id, int totalCases, Instant startedAt) {
        this.id = id;
        this.status = "RUNNING";
        this.totalCases = totalCases;
        this.passedCases = 0;
        this.passRate = BigDecimal.ZERO;
        this.startedAt = startedAt;
    }

    public void complete(int passedCases, BigDecimal passRate, Instant completedAt) {
        this.status = "COMPLETED";
        this.passedCases = passedCases;
        this.passRate = passRate;
        this.completedAt = completedAt;
    }

    public void fail(String message, Instant completedAt) {
        this.status = "FAILED";
        this.completedAt = completedAt;
        this.failureMessage = message;
    }

    public UUID getId() { return id; }
    public String getStatus() { return status; }
    public int getTotalCases() { return totalCases; }
    public int getPassedCases() { return passedCases; }
    public BigDecimal getPassRate() { return passRate; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public String getFailureMessage() { return failureMessage; }
}
