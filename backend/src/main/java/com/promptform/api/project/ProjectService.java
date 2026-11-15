package com.promptform.api.project;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptform.api.generation.GenerationModels.UiSchema;
import com.promptform.api.generation.SchemaValidator;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {
    private static final int MAX_SCHEMA_BYTES = 200_000;
    private final ProjectRepository projectRepository;
    private final VersionRepository versionRepository;
    private final ObjectMapper objectMapper;
    private final SchemaValidator schemaValidator;

    public ProjectService(ProjectRepository projectRepository, VersionRepository versionRepository,
                          ObjectMapper objectMapper, SchemaValidator schemaValidator) {
        this.projectRepository = projectRepository;
        this.versionRepository = versionRepository;
        this.objectMapper = objectMapper;
        this.schemaValidator = schemaValidator;
    }

    @Transactional
    public ProjectModels.ProjectResponse createProject(UUID userId, ProjectModels.CreateProjectRequest request) {
        ProjectEntity project = new ProjectEntity(UUID.randomUUID(), userId, request.name().trim(), Instant.now());
        return toProjectResponse(projectRepository.save(project));
    }

    @Transactional(readOnly = true)
    public List<ProjectModels.ProjectResponse> listProjects(UUID userId) {
        return projectRepository.findTop100ByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(this::toProjectResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectModels.ProjectDetails getProject(UUID userId, UUID projectId) {
        ProjectEntity project = findOwnedProject(userId, projectId);
        List<ProjectModels.VersionResponse> versions = versionRepository
                .findTop100ByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::toVersionResponse)
                .toList();
        return new ProjectModels.ProjectDetails(toProjectResponse(project), versions);
    }

    @Transactional
    public ProjectModels.VersionResponse createVersion(UUID userId, UUID projectId,
                                                       ProjectModels.CreateVersionRequest request) {
        ProjectEntity project = findOwnedProject(userId, projectId);
        validateSchema(request.schema());
        if (request.parentVersionId() != null
                && versionRepository.findByIdAndProjectId(request.parentVersionId(), projectId).isEmpty()) {
            throw new ProjectException("The parent version does not belong to this project.");
        }

        VersionEntity version = new VersionEntity(
                UUID.randomUUID(), projectId, request.parentVersionId(), request.prompt().trim(),
                request.schema(), Instant.now());
        project.markUpdated(Instant.now());
        projectRepository.save(project);
        return toVersionResponse(versionRepository.save(version));
    }

    private ProjectEntity findOwnedProject(UUID userId, UUID projectId) {
        return projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ProjectException("Project was not found."));
    }

    private void validateSchema(JsonNode schemaJson) {
        if (schemaJson.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_SCHEMA_BYTES) {
            throw new ProjectValidationException("The form schema is too large.");
        }
        try {
            UiSchema schema = objectMapper.treeToValue(schemaJson, UiSchema.class);
            schemaValidator.validate(schema);
        } catch (ProjectValidationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ProjectValidationException("The form schema is invalid.");
        }
    }

    private ProjectModels.ProjectResponse toProjectResponse(ProjectEntity project) {
        return new ProjectModels.ProjectResponse(
                project.getId(), project.getName(), project.getCreatedAt(), project.getUpdatedAt());
    }

    private ProjectModels.VersionResponse toVersionResponse(VersionEntity version) {
        JsonNode schema = version.getSchema();
        return new ProjectModels.VersionResponse(
                version.getId(), version.getProjectId(), version.getParentVersionId(),
                version.getPrompt(), schema, version.getCreatedAt());
    }
}
