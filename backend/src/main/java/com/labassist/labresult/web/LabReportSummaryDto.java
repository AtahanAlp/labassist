package com.labassist.labresult.web;

import com.labassist.common.domain.Sex;
import com.labassist.labresult.domain.ReportStatus;
import java.time.Instant;
import java.util.UUID;

/** Row in the report list. Patient name/MRN are decrypted for the authorized doctor. */
public record LabReportSummaryDto(
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
        int criticalCount) {
}
