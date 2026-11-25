package com.promptform.api.submission;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptform.api.generation.SchemaValidator;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SubmissionService {
    private static final int MAX_JSON_BYTES = 200_000;
    private final SubmissionRepository repository;
    private final SchemaValidator schemaValidator;
    private final SubmissionValueValidator valueValidator;
    private final ObjectMapper objectMapper;

    public SubmissionService(SubmissionRepository repository, SchemaValidator schemaValidator,
                             SubmissionValueValidator valueValidator, ObjectMapper objectMapper) {
        this.repository = repository;
        this.schemaValidator = schemaValidator;
        this.valueValidator = valueValidator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SubmissionModels.SubmissionResponse create(SubmissionModels.CreateSubmissionRequest request) {
        if (request.schema() == null || request.values() == null || !request.values().isObject()) {
            throw new SubmissionException("A form schema and an object of submitted values are required.");
        }
        ensureSize(request.schema(), "form schema");
        ensureSize(request.values(), "submitted values");
        try {
            schemaValidator.validate(request.schema());
            valueValidator.validate(request.schema(), request.values());
        } catch (RuntimeException exception) {
            if (exception instanceof SubmissionException) throw exception;
            throw new SubmissionException("The submitted form schema is invalid.", exception);
        }

        Instant submittedAt = Instant.now();
        SubmissionEntity submission = repository.save(new SubmissionEntity(
                UUID.randomUUID(), objectMapper.valueToTree(request.schema()), request.values(), submittedAt));
        return new SubmissionModels.SubmissionResponse(submission.getId(), submission.getSubmittedAt());
    }

    private void ensureSize(Object value, String label) {
        int size = objectMapper.valueToTree(value).toString().getBytes(StandardCharsets.UTF_8).length;
        if (size > MAX_JSON_BYTES) throw new SubmissionException("The " + label + " is too large.");
    }
}
