package com.labassist.labresult.reference;

import com.labassist.common.domain.Sex;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Canonical reference range for an analyte — the backend's own source of truth
 * for abnormality flagging (it does not trust ranges sent by the device).
 *
 * <p>{@code sex}/{@code ageMin}/{@code ageMax} narrow applicability; {@code null}
 * means "applies to any". More specific rows are preferred at evaluation time.
 */
@Getter
@Setter
@Entity
@Table(name = "reference_range")
public class ReferenceRange {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 32)
    private String code;

    @Column(length = 128)
    private String name;

    @Column(length = 32)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private Sex sex;

    @Column(name = "age_min")
    private Integer ageMin;

    @Column(name = "age_max")
    private Integer ageMax;

    @Column(precision = 14, scale = 3)
    private BigDecimal low;

    @Column(precision = 14, scale = 3)
    private BigDecimal high;

    @Column(name = "critical_low", precision = 14, scale = 3)
    private BigDecimal criticalLow;

    @Column(name = "critical_high", precision = 14, scale = 3)
    private BigDecimal criticalHigh;
}
