package com.labassist.llm;

import com.labassist.llm.domain.LlmInterpretation;
import com.labassist.llm.domain.LlmStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LlmInterpretationRepository extends JpaRepository<LlmInterpretation, UUID> {

    Optional<LlmInterpretation> findFirstByReport_IdAndStatusOrderByCreatedAtDesc(UUID reportId, LlmStatus status);

    Optional<LlmInterpretation> findFirstByReport_IdOrderByCreatedAtDesc(UUID reportId);
}
