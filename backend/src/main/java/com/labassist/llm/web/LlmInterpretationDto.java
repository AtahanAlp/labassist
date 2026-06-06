package com.labassist.llm.web;

import com.labassist.llm.domain.LlmInterpretation;
import com.labassist.llm.domain.LlmStatus;
import java.time.Instant;
import java.util.UUID;

/** LLM interpretation returned to the client. */
public record LlmInterpretationDto(
        UUID id,
        UUID reportId,
        String model,
        LlmStatus status,
        String responseText,
        Long latencyMs,
        String createdBy,
        Instant createdAt,
        String errorMessage) {

    public static LlmInterpretationDto from(LlmInterpretation interpretation) {
        return new LlmInterpretationDto(
                interpretation.getId(),
                interpretation.getReport().getId(),
                interpretation.getModel(),
                interpretation.getStatus(),
                interpretation.getResponseText(),
                interpretation.getLatencyMs(),
                interpretation.getCreatedBy(),
                interpretation.getCreatedAt(),
                interpretation.getErrorMessage());
    }
}
