package com.promptform.api.form;

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
@Table(name = "forms")
public class FormEntity {
    @Id
    private UUID id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "schema_json", nullable = false, columnDefinition = "jsonb")
    private JsonNode schema;

    @Column(name = "unique_field_name", nullable = false)
    private String uniqueFieldName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "approved_at", nullable = false)
    private Instant approvedAt;

    protected FormEntity() { }

    public FormEntity(UUID id, JsonNode schema, String uniqueFieldName, Instant createdAt, Instant approvedAt) {
        this.id = id;
        this.schema = schema;
        this.uniqueFieldName = uniqueFieldName;
        this.createdAt = createdAt;
        this.approvedAt = approvedAt;
    }

    public UUID getId() { return id; }
    public JsonNode getSchema() { return schema; }
    public String getUniqueFieldName() { return uniqueFieldName; }
    public Instant getApprovedAt() { return approvedAt; }
}
