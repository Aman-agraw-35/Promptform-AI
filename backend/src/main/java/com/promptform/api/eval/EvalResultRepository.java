package com.promptform.api.eval;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvalResultRepository extends JpaRepository<EvalResultEntity, UUID> {
    List<EvalResultEntity> findAllByRunIdOrderByCreatedAtAsc(UUID runId);
}
