package com.promptform.api.form;

import com.fasterxml.jackson.databind.JsonNode;
import com.promptform.api.generation.GenerationModels.UiSchema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public final class FormModels {
    private FormModels() { }

    public record ApproveFormRequest(@Valid @NotNull UiSchema schema) { }
    public record CreateSubmissionRequest(@NotNull JsonNode values) { }
    public record FormResponse(UUID id, JsonNode schema, String uniqueFieldName, Instant approvedAt, String sharePath) { }
    public record SubmissionResponse(UUID id, Instant submittedAt) { }
    public record SubmissionDetails(UUID id, Instant submittedAt, JsonNode values) { }
}
