package com.labassist.ingestion;

/** Result of ingesting a single device message. */
public record IngestionOutcome(Type type, boolean abnormal, boolean critical, boolean partial) {

    public enum Type {
        STORED,
        SKIPPED_DUPLICATE,
        REJECTED
    }

    public static IngestionOutcome stored(boolean abnormal, boolean critical, boolean partial) {
        return new IngestionOutcome(Type.STORED, abnormal, critical, partial);
    }

    public static IngestionOutcome skipped() {
        return new IngestionOutcome(Type.SKIPPED_DUPLICATE, false, false, false);
    }

    public static IngestionOutcome rejected() {
        return new IngestionOutcome(Type.REJECTED, false, false, false);
    }
}
