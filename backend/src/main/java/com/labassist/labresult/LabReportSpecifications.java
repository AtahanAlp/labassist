package com.labassist.labresult;

import com.labassist.labresult.domain.LabReport;
import com.labassist.labresult.domain.ReportStatus;
import org.springframework.data.jpa.domain.Specification;

/**
 * Reusable filters for the report list.
 *
 * <p>Note: patient name/MRN are encrypted at rest, so they cannot be filtered in
 * SQL. Free-text search therefore matches {@code external_id} only. (A blind index
 * would enable encrypted-field search — see README "what we didn't do".)
 */
public final class LabReportSpecifications {

    private LabReportSpecifications() {
    }

    public static Specification<LabReport> abnormalOnly(Boolean abnormalOnly) {
        return (root, query, cb) ->
                Boolean.TRUE.equals(abnormalOnly) ? cb.isTrue(root.get("overallAbnormal")) : cb.conjunction();
    }

    public static Specification<LabReport> hasStatus(ReportStatus status) {
        return (root, query, cb) ->
                status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<LabReport> externalIdContains(String text) {
        return (root, query, cb) -> (text == null || text.isBlank())
                ? cb.conjunction()
                : cb.like(cb.lower(root.get("externalId")), "%" + text.toLowerCase() + "%");
    }
}
