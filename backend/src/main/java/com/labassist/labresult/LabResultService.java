package com.labassist.labresult;

import com.labassist.audit.AuditService;
import com.labassist.audit.domain.AuditAction;
import com.labassist.common.exception.NotFoundException;
import com.labassist.common.web.PagedResponse;
import com.labassist.labresult.domain.LabReport;
import com.labassist.labresult.domain.ReportStatus;
import com.labassist.labresult.web.LabReportDetailDto;
import com.labassist.labresult.web.LabReportSummaryDto;
import com.labassist.labresult.web.ReportSummaryDto;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read API for lab reports. Decryption of PII happens transparently via the entity. */
@Service
public class LabResultService {

    private final LabReportRepository reportRepository;
    private final AuditService auditService;

    public LabResultService(LabReportRepository reportRepository, AuditService auditService) {
        this.reportRepository = reportRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PagedResponse<LabReportSummaryDto> list(Boolean abnormalOnly, Boolean criticalOnly, ReportStatus status,
                                                   String query, Instant from, Instant to,
                                                   boolean includeRejected, Pageable pageable) {
        Specification<LabReport> spec = visibleReports(includeRejected)
                .and(LabReportSpecifications.abnormalOnly(abnormalOnly))
                .and(LabReportSpecifications.criticalOnly(criticalOnly))
                .and(LabReportSpecifications.hasStatus(status))
                .and(LabReportSpecifications.externalIdContains(query))
                .and(LabReportSpecifications.receivedFrom(from))
                .and(LabReportSpecifications.receivedBefore(to));
        Page<LabReportSummaryDto> page = reportRepository.findAll(spec, pageable).map(LabReportMapper::toSummary);
        return PagedResponse.from(page);
    }

    @Transactional(readOnly = true)
    public ReportSummaryDto summary(boolean includeRejected) {
        Specification<LabReport> visible = visibleReports(includeRejected);
        long total = reportRepository.count(visible);
        long abnormal = reportRepository.count(visible.and(LabReportSpecifications.abnormalOnly(true)));
        long critical = reportRepository.count(visible.and(LabReportSpecifications.criticalOnly(true)));
        long partial = reportRepository.count(visible.and(LabReportSpecifications.hasStatus(ReportStatus.PARTIAL)));
        long rejected = reportRepository.count(LabReportSpecifications.hasStatus(ReportStatus.REJECTED));
        return new ReportSummaryDto(total, abnormal, critical, partial, rejected);
    }

    @Transactional(readOnly = true)
    public LabReportDetailDto get(UUID id, String actor, String ipAddress, boolean includeRejected) {
        LabReport report = reportRepository.findWithTestsById(id)
                .orElseThrow(() -> new NotFoundException("Lab report not found: " + id));
        // Doctors don't get to see rejected (malformed) reports even by direct id.
        if (!includeRejected && report.getStatus() == ReportStatus.REJECTED) {
            throw new NotFoundException("Lab report not found: " + id);
        }
        auditService.success(AuditAction.REPORT_VIEW, actor, "LabReport", id.toString(), null, ipAddress);
        return LabReportMapper.toDetail(report);
    }

    private Specification<LabReport> visibleReports(boolean includeRejected) {
        return Specification.where(LabReportSpecifications.excludeRejected(!includeRejected));
    }
}
