package com.labassist.labresult;

import com.labassist.labresult.domain.LabReport;
import com.labassist.labresult.domain.TestResult;
import com.labassist.labresult.web.LabReportDetailDto;
import com.labassist.labresult.web.LabReportSummaryDto;
import com.labassist.labresult.web.TestResultDto;
import java.util.Comparator;
import java.util.List;

/** Maps {@link LabReport} entities to API DTOs (PII is decrypted via the entity getters). */
public final class LabReportMapper {

    private LabReportMapper() {
    }

    public static LabReportSummaryDto toSummary(LabReport report) {
        return new LabReportSummaryDto(
                report.getId(),
                report.getExternalId(),
                report.getPatientName(),
                report.getPatientMrn(),
                report.getPatientAge(),
                report.getPatientSex(),
                report.getDeviceId(),
                report.getSampleCollectedAt(),
                report.getReceivedAt(),
                report.getStatus(),
                report.isOverallAbnormal(),
                report.getAbnormalCount(),
                report.getCriticalCount());
    }

    public static LabReportDetailDto toDetail(LabReport report) {
        List<TestResultDto> tests = report.getTests().stream()
                .sorted(Comparator.comparing(TestResult::getCode))
                .map(LabReportMapper::toTestDto)
                .toList();
        return new LabReportDetailDto(
                report.getId(),
                report.getExternalId(),
                report.getPatientName(),
                report.getPatientMrn(),
                report.getPatientAge(),
                report.getPatientSex(),
                report.getDeviceId(),
                report.getSampleCollectedAt(),
                report.getReceivedAt(),
                report.getStatus(),
                report.isOverallAbnormal(),
                report.getAbnormalCount(),
                report.getCriticalCount(),
                report.getRejectionReason(),
                tests);
    }

    private static TestResultDto toTestDto(TestResult test) {
        return new TestResultDto(
                test.getCode(),
                test.getName(),
                test.getValue(),
                test.getUnit(),
                test.getRefLow(),
                test.getRefHigh(),
                test.getFlag());
    }
}
