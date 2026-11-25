CREATE TABLE forms (
    id UUID PRIMARY KEY,
    schema_json JSONB NOT NULL,
    unique_field_name VARCHAR(160) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    approved_at TIMESTAMPTZ NOT NULL
);

ALTER TABLE form_submissions
    ADD COLUMN form_id UUID REFERENCES forms (id) ON DELETE CASCADE,
    ADD COLUMN unique_value VARCHAR(320);

CREATE INDEX idx_form_submissions_form_id ON form_submissions (form_id);
CREATE UNIQUE INDEX uq_form_submissions_form_unique_value
    ON form_submissions (form_id, unique_value)
    WHERE form_id IS NOT NULL AND unique_value IS NOT NULL;
