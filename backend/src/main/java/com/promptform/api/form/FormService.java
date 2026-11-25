package com.promptform.api.form;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptform.api.generation.GenerationModels.UiNode;
import com.promptform.api.generation.GenerationModels.UiSchema;
import com.promptform.api.generation.SchemaValidator;
import com.promptform.api.submission.SubmissionEntity;
import com.promptform.api.submission.SubmissionRepository;
import com.promptform.api.submission.SubmissionValueValidator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FormService {
    private final FormRepository formRepository;
    private final SubmissionRepository submissionRepository;
    private final SchemaValidator schemaValidator;
    private final SubmissionValueValidator valueValidator;
    private final ObjectMapper objectMapper;

    public FormService(FormRepository formRepository, SubmissionRepository submissionRepository,
                       SchemaValidator schemaValidator, SubmissionValueValidator valueValidator,
                       ObjectMapper objectMapper) {
        this.formRepository = formRepository;
        this.submissionRepository = submissionRepository;
        this.schemaValidator = schemaValidator;
        this.valueValidator = valueValidator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public FormModels.FormResponse approve(FormModels.ApproveFormRequest request) {
        UiSchema schema = request.schema();
        schemaValidator.validate(schema);
        List<String> uniqueFields = new ArrayList<>();
        collectUniqueFields(schema.children(), uniqueFields);
        if (uniqueFields.size() != 1) {
            throw new FormException("Choose exactly one unique field before approving the form.");
        }

        Instant now = Instant.now();
        FormEntity form = formRepository.save(new FormEntity(
                UUID.randomUUID(), objectMapper.valueToTree(schema), uniqueFields.get(0), now, now));
        return toResponse(form);
    }

    @Transactional(readOnly = true)
    public FormModels.FormResponse get(UUID formId) {
        return toResponse(formRepository.findById(formId)
                .orElseThrow(() -> new FormException("The approved form was not found.")));
    }

    @Transactional(readOnly = true)
    public List<FormModels.SubmissionDetails> listSubmissions(UUID formId) {
        formRepository.findById(formId)
                .orElseThrow(() -> new FormException("The approved form was not found."));
        return submissionRepository.findTop500ByFormIdOrderBySubmittedAtDesc(formId).stream()
                .map(submission -> new FormModels.SubmissionDetails(
                        submission.getId(), submission.getSubmittedAt(), submission.getValues()))
                .toList();
    }

    @Transactional
    public FormModels.SubmissionResponse submit(UUID formId, FormModels.CreateSubmissionRequest request) {
        FormEntity form = formRepository.findById(formId)
                .orElseThrow(() -> new FormException("The approved form was not found."));
        if (request.values() == null || !request.values().isObject()) {
            throw new FormException("Submitted values must be a JSON object.");
        }

        try {
            UiSchema schema = objectMapper.treeToValue(form.getSchema(), UiSchema.class);
            valueValidator.validate(schema, request.values());
        } catch (FormException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new FormException("The approved form schema could not be validated.", exception);
        }

        JsonNode uniqueValueNode = request.values().get(form.getUniqueFieldName());
        String uniqueValue = normalizeUniqueValue(uniqueValueNode, form.getUniqueFieldName());
        Instant submittedAt = Instant.now();
        try {
            SubmissionEntity submission = submissionRepository.save(new SubmissionEntity(
                    UUID.randomUUID(), form.getId(), form.getSchema(), request.values(),
                    uniqueValue, submittedAt));
            return new FormModels.SubmissionResponse(submission.getId(), submittedAt);
        } catch (DataIntegrityViolationException exception) {
            throw new FormException("This form has already been submitted with that unique value.", exception);
        }
    }

    private void collectUniqueFields(Iterable<UiNode> nodes, List<String> uniqueFields) {
        for (UiNode node : nodes) {
            if (node.children() != null) collectUniqueFields(node.children(), uniqueFields);
            if (node.name() != null && Boolean.TRUE.equals(node.unique())) uniqueFields.add(node.name());
        }
    }

    private String normalizeUniqueValue(JsonNode value, String fieldName) {
        if (value == null || value.isNull() || value.isContainerNode() || value.asText().isBlank()) {
            throw new FormException("The unique field '" + fieldName + "' is required.");
        }
        return value.isTextual()
                ? value.asText().trim().toLowerCase(Locale.ROOT)
                : value.asText();
    }

    private FormModels.FormResponse toResponse(FormEntity form) {
        return new FormModels.FormResponse(form.getId(), form.getSchema(), form.getUniqueFieldName(),
                form.getApprovedAt(), "/forms/" + form.getId());
    }
}
