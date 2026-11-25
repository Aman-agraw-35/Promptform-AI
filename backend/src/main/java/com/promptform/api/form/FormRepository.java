package com.promptform.api.form;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormRepository extends JpaRepository<FormEntity, UUID> {
    boolean existsByIdAndUniqueFieldName(UUID id, String uniqueFieldName);
}
