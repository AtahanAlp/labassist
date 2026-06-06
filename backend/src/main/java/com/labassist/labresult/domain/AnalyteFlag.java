package com.labassist.labresult.domain;

/** Abnormality classification of a single analyte value against its reference range. */
public enum AnalyteFlag {
    NORMAL,
    LOW,
    HIGH,
    CRITICAL_LOW,
    CRITICAL_HIGH,
    /** No value measured, or no reference range available. */
    UNKNOWN;

    public boolean isAbnormal() {
        return this == LOW || this == HIGH || this == CRITICAL_LOW || this == CRITICAL_HIGH;
    }

    public boolean isCritical() {
        return this == CRITICAL_LOW || this == CRITICAL_HIGH;
    }
}
