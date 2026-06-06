package com.labassist.labresult.domain;

import com.labassist.common.domain.BaseEntity;
import com.labassist.common.domain.Sex;
import com.labassist.crypto.PiiAttributeConverter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One lab report received from a device for a single patient, containing one or
 * more analyte measurements. Patient name and MRN are encrypted at rest.
 */
@Getter
@Setter
@Entity
@Table(name = "lab_report")
public class LabReport extends BaseEntity {

    /** Device-assigned message id; unique, used for idempotent ingestion. */
    @Column(name = "external_id", nullable = false, unique = true, length = 64)
    private String externalId;

    @Column(name = "device_id", length = 64)
    private String deviceId;

    @Convert(converter = PiiAttributeConverter.class)
    @Column(name = "patient_name", length = 512)
    private String patientName;

    @Convert(converter = PiiAttributeConverter.class)
    @Column(name = "patient_mrn", length = 512)
    private String patientMrn;

    @Column(name = "patient_age")
    private Integer patientAge;

    @Enumerated(EnumType.STRING)
    @Column(name = "patient_sex", length = 16)
    private Sex patientSex;

    @Column(name = "sample_collected_at")
    private Instant sampleCollectedAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReportStatus status;

    @Column(name = "overall_abnormal", nullable = false)
    private boolean overallAbnormal;

    @Column(name = "abnormal_count", nullable = false)
    private int abnormalCount;

    @Column(name = "critical_count", nullable = false)
    private int criticalCount;

    /** Original device payload, retained verbatim for audit/debugging. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb")
    private String rawPayload;

    @Column(name = "rejection_reason", length = 512)
    private String rejectionReason;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestResult> tests = new ArrayList<>();

    public void addTest(TestResult test) {
        test.setReport(this);
        this.tests.add(test);
    }
}
