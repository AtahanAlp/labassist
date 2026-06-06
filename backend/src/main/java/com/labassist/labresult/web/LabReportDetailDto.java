package com.labassist.labresult.web;

import com.labassist.common.domain.Sex;
import com.labassist.labresult.domain.ReportStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Full report detail including analytes and (if rejected) the reason. */
public record LabReportDetailDto(
        UUID id,
        String externalId,
        String patientName,
        String patientMrn,
        Integer patientAge,
        Sex patientSex,
        String deviceId,
        Instant sampleCollectedAt,
        Instant receivedAt,
        ReportStatus status,
        boolean overallAbnormal,
        int abnormalCount,
        int criticalCount,
        String rejectionReason,
        List<TestResultDto> tests) {
}
