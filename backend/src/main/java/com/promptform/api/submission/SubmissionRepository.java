package com.promptform.api.submission;

import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionRepository extends JpaRepository<SubmissionEntity, UUID> {
    List<SubmissionEntity> findTop500ByFormIdOrderBySubmittedAtDesc(UUID formId);
}
