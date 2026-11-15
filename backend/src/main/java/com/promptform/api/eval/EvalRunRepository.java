package com.promptform.api.eval;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvalRunRepository extends JpaRepository<EvalRunEntity, UUID> {
    List<EvalRunEntity> findTop20ByOrderByStartedAtDesc();
}
