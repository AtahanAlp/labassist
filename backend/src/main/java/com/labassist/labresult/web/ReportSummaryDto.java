package com.labassist.labresult.web;

/** Aggregate counts for the dashboard header (scoped to the caller's visibility). */
public record ReportSummaryDto(
        long total,
        long abnormal,
        long critical,
        long partial,
        long rejected) {
}
