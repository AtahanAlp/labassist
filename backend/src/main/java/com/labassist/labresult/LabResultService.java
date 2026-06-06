package com.labassist.labresult;

import com.labassist.audit.AuditService;
import com.labassist.audit.domain.AuditAction;
import com.labassist.common.exception.NotFoundException;
import com.labassist.common.web.PagedResponse;
import com.labassist.labresult.domain.LabReport;
import com.labassist.labresult.domain.ReportStatus;
import com.labassist.labresult.web.LabReportDetailDto;
import com.labassist.labresult.web.LabReportSummaryDto;
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
    public PagedResponse<LabReportSummaryDto> list(Boolean abnormalOnly, ReportStatus status,
                                                   String query, Pageable pageable) {
        Specification<LabReport> spec = Specification
                .where(LabReportSpecifications.abnormalOnly(abnormalOnly))
                .and(LabReportSpecifications.hasStatus(status))
                .and(LabReportSpecifications.externalIdContains(query));
        Page<LabReportSummaryDto> page = reportRepository.findAll(spec, pageable).map(LabReportMapper::toSummary);
        return PagedResponse.from(page);
    }

    @Transactional(readOnly = true)
    public LabReportDetailDto get(UUID id, String actor, String ipAddress) {
        LabReport report = reportRepository.findWithTestsById(id)
                .orElseThrow(() -> new NotFoundException("Lab report not found: " + id));
        auditService.success(AuditAction.REPORT_VIEW, actor, "LabReport", id.toString(), null, ipAddress);
        return LabReportMapper.toDetail(report);
    }
}
