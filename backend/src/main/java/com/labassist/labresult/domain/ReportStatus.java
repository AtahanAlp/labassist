package com.labassist.labresult.domain;

/** Lifecycle state of an ingested lab report. */
public enum ReportStatus {
    /** Accepted and stored, all analytes measured. */
    VALIDATED,
    /** Accepted but some analyte values were missing. */
    PARTIAL,
    /** Structurally invalid payload that failed validation. */
    REJECTED
}
