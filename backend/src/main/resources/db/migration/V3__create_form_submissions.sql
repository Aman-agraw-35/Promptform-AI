CREATE TABLE form_submissions (
    id UUID PRIMARY KEY,
    schema_json JSONB NOT NULL,
    values_json JSONB NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_form_submissions_submitted_at ON form_submissions (submitted_at DESC);
