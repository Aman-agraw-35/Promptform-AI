package com.promptform.api.project;

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
@Table(name = "versions")
public class VersionEntity {
    @Id
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "parent_version_id")
    private UUID parentVersionId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "schema_json", nullable = false, columnDefinition = "jsonb")
    private JsonNode schema;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected VersionEntity() { }

    public VersionEntity(UUID id, UUID projectId, UUID parentVersionId, String prompt,
                         JsonNode schema, Instant createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.parentVersionId = parentVersionId;
        this.prompt = prompt;
        this.schema = schema;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getProjectId() { return projectId; }
    public UUID getParentVersionId() { return parentVersionId; }
    public String getPrompt() { return prompt; }
    public JsonNode getSchema() { return schema; }
    public Instant getCreatedAt() { return createdAt; }
}
