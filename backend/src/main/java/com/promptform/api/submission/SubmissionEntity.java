package com.promptform.api.submission;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "form_submissions")
public class SubmissionEntity {
    @Id
    private UUID id;

    @Column(name = "form_id")
    private UUID formId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "schema_json", nullable = false, columnDefinition = "jsonb")
    private JsonNode schema;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "values_json", nullable = false, columnDefinition = "jsonb")
    private JsonNode values;

    @Column(name = "unique_value")
    private String uniqueValue;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    protected SubmissionEntity() { }

    public SubmissionEntity(UUID id, JsonNode schema, JsonNode values, Instant submittedAt) {
        this(id, null, schema, values, null, submittedAt);
    }

    public SubmissionEntity(UUID id, UUID formId, JsonNode schema, JsonNode values,
                            String uniqueValue, Instant submittedAt) {
        this.id = id;
        this.formId = formId;
        this.schema = schema;
        this.values = values;
        this.uniqueValue = uniqueValue;
        this.submittedAt = submittedAt;
    }

    public UUID getId() { return id; }
    public JsonNode getValues() { return values; }
    public Instant getSubmittedAt() { return submittedAt; }
}
