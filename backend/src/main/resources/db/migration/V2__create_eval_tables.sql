CREATE TABLE eval_runs (
    id UUID PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    total_cases INTEGER NOT NULL,
    passed_cases INTEGER NOT NULL DEFAULT 0,
    pass_rate NUMERIC(5, 4) NOT NULL DEFAULT 0,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    failure_message TEXT
);

CREATE INDEX idx_eval_runs_started_at ON eval_runs (started_at DESC);

CREATE TABLE eval_results (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES eval_runs (id) ON DELETE CASCADE,
    case_id VARCHAR(80) NOT NULL,
    prompt TEXT NOT NULL,
    schema_valid BOOLEAN NOT NULL,
    renders_without_errors BOOLEAN NOT NULL,
    requirement_score NUMERIC(5, 4) NOT NULL,
    passed BOOLEAN NOT NULL,
    latency_ms BIGINT NOT NULL,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (run_id, case_id)
);

CREATE INDEX idx_eval_results_run_id ON eval_results (run_id);
