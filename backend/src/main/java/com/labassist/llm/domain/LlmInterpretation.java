package com.labassist.llm.domain;

import com.labassist.common.domain.BaseEntity;
import com.labassist.labresult.domain.LabReport;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** A stored LLM interpretation for a report — an auditable, cacheable record. */
@Getter
@Setter
@Entity
@Table(name = "llm_interpretation")
public class LlmInterpretation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lab_report_id", nullable = false)
    private LabReport report;

    @Column(length = 64)
    private String model;

    @Column(name = "prompt_version", length = 32)
    private String promptVersion;

    @Column(name = "response_text", columnDefinition = "text")
    private String responseText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private LlmStatus status;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Column(name = "error_message", length = 1024)
    private String errorMessage;
}
