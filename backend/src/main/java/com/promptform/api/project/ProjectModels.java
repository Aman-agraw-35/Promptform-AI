package com.promptform.api.project;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ProjectModels {
    private ProjectModels() { }

    public record CreateProjectRequest(@NotBlank @Size(max = 160) String name) { }

    public record CreateVersionRequest(
            @NotBlank @Size(max = 2000) String prompt,
            @NotNull JsonNode schema,
            UUID parentVersionId) { }

    public record ProjectResponse(UUID id, String name, Instant createdAt, Instant updatedAt) { }

    public record VersionResponse(UUID id, UUID projectId, UUID parentVersionId, String prompt,
                                  JsonNode schema, Instant createdAt) { }

    public record ProjectDetails(ProjectResponse project, List<VersionResponse> versions) { }
}
