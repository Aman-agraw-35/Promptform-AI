package com.promptform.api.submission;

import com.fasterxml.jackson.databind.JsonNode;
import com.promptform.api.generation.GenerationModels.UiSchema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public final class SubmissionModels {
    private SubmissionModels() { }

    public record CreateSubmissionRequest(
            @NotNull UiSchema schema,
            @NotNull JsonNode values) { }

    public record SubmissionResponse(UUID id, Instant submittedAt) { }
}
