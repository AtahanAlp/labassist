package com.labassist.ingestion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically triggers an ingestion poll. Resilient by design: any failure
 * (device down, transient 500, timeout) is logged and retried next cycle, leaving
 * the cursor untouched so no data is lost.
 *
 * <p>Disabled in tests via {@code labassist.lab-device.polling-enabled=false}.
 */
@Slf4j
@Component
@ConditionalOnProperty(value = "labassist.lab-device.polling-enabled", havingValue = "true", matchIfMissing = true)
public class LabResultPoller {

    private final IngestionService ingestionService;

    public LabResultPoller(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @Scheduled(
            fixedDelayString = "${labassist.lab-device.poll-interval-ms}",
            initialDelayString = "${labassist.lab-device.poll-interval-ms}")
    public void poll() {
        try {
            ingestionService.poll();
        } catch (Exception e) {
            log.warn("Lab device poll failed (will retry next cycle): {}", e.getMessage());
        }
    }
}
