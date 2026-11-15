package com.promptform.api.project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<ProjectEntity, UUID> {
    List<ProjectEntity> findTop100ByUserIdOrderByUpdatedAtDesc(UUID userId);
    Optional<ProjectEntity> findByIdAndUserId(UUID id, UUID userId);
}
