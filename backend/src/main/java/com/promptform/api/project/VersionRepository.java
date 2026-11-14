package com.promptform.api.project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VersionRepository extends JpaRepository<VersionEntity, UUID> {
    List<VersionEntity> findAllByProjectIdOrderByCreatedAtDesc(UUID projectId);
    Optional<VersionEntity> findByIdAndProjectId(UUID id, UUID projectId);
}
