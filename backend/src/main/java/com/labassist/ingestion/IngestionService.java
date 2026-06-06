package com.labassist.ingestion;

import com.fasterxml.jackson.databind.JsonNode;
import com.labassist.audit.AuditService;
import com.labassist.audit.domain.AuditAction;
import com.labassist.ingestion.client.DeviceBatch;
import com.labassist.ingestion.client.LabDeviceClient;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Orchestrates one poll cycle: fetch a batch from the device, ingest each message
 * independently, advance the cursor, and audit the result.
 *
 * <p>The cursor is kept in memory; after a restart it resets to 0 and re-fetches,
 * relying on idempotent ingestion ({@code external_id}) to skip already-stored
 * reports — simple and correct, at the cost of one re-scan on restart.
 */
@Slf4j
@Service
public class IngestionService {

    private final LabDeviceClient deviceClient;
    private final MessageIngestor messageIngestor;
    private final AuditService auditService;
    private final AtomicLong cursor = new AtomicLong(0);

    public IngestionService(LabDeviceClient deviceClient, MessageIngestor messageIngestor,
                            AuditService auditService) {
        this.deviceClient = deviceClient;
        this.messageIngestor = messageIngestor;
        this.auditService = auditService;
    }

    public IngestionSummary poll() {
        DeviceBatch batch = deviceClient.fetchSince(cursor.get());

        int stored = 0;
        int skipped = 0;
        int rejected = 0;
        int partial = 0;
        int abnormal = 0;
        int critical = 0;

        for (JsonNode node : batch.results()) {
            IngestionOutcome outcome;
            try {
                outcome = messageIngestor.ingest(node);
            } catch (Exception e) {
                log.warn("Failed to ingest a message; skipping it", e);
                continue;
            }
            switch (outcome.type()) {
                case STORED -> {
                    stored++;
                    if (outcome.partial()) {
                        partial++;
                    }
                    if (outcome.abnormal()) {
                        abnormal++;
                    }
                    if (outcome.critical()) {
                        critical++;
                    }
                }
                case SKIPPED_DUPLICATE -> skipped++;
                case REJECTED -> rejected++;
            }
        }

        cursor.set(batch.cursor());
        IngestionSummary summary = new IngestionSummary(
                batch.results().size(), stored, skipped, rejected, partial, abnormal, critical);

        if (!batch.results().isEmpty()) {
            log.info("Ingest poll: {}", summary);
            auditService.success(AuditAction.INGEST_POLL, "system", "LabReport", null, summary.toMap(), null);
        }
        return summary;
    }

    /** Test/diagnostic accessor for the current cursor position. */
    public long currentCursor() {
        return cursor.get();
    }
}
