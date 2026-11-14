package com.promptform.api.project;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectModels.ProjectResponse create(@AuthenticationPrincipal Jwt jwt,
                                                @Valid @RequestBody ProjectModels.CreateProjectRequest request) {
        return projectService.createProject(userId(jwt), request);
    }

    @GetMapping
    public List<ProjectModels.ProjectResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return projectService.listProjects(userId(jwt));
    }

    @GetMapping("/{projectId}")
    public ProjectModels.ProjectDetails get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId) {
        return projectService.getProject(userId(jwt), projectId);
    }

    @PostMapping("/{projectId}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectModels.VersionResponse createVersion(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID projectId,
            @Valid @RequestBody ProjectModels.CreateVersionRequest request) {
        return projectService.createVersion(userId(jwt), projectId, request);
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
