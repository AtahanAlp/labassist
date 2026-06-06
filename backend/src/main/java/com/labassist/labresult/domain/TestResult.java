package com.labassist.labresult.domain;

import com.labassist.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/** A single analyte measurement within a {@link LabReport}. */
@Getter
@Setter
@Entity
@Table(name = "test_result")
public class TestResult extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lab_report_id", nullable = false)
    private LabReport report;

    @Column(nullable = false, length = 32)
    private String code;

    @Column(length = 128)
    private String name;

    /** Numeric result; null when not measured or non-numeric. */
    @Column(precision = 14, scale = 3)
    private BigDecimal value;

    /** Non-numeric/qualitative result, if any. */
    @Column(name = "value_text", length = 128)
    private String valueText;

    @Column(length = 32)
    private String unit;

    @Column(name = "ref_low", precision = 14, scale = 3)
    private BigDecimal refLow;

    @Column(name = "ref_high", precision = 14, scale = 3)
    private BigDecimal refHigh;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AnalyteFlag flag;
}
