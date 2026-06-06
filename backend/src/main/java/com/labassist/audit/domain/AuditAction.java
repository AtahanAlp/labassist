package com.labassist.audit.domain;

/** The set of auditable actions recorded in the audit trail. */
public enum AuditAction {
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    INGEST_POLL,
    REPORT_LIST,
    REPORT_VIEW,
    LLM_INTERPRET
}
