package com.labassist.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Connection settings for the external lab-device mock service that the
 * ingestion poller fetches results from.
 */
@ConfigurationProperties("labassist.lab-device")
public record LabDeviceProperties(
        String baseUrl,
        long pollIntervalMs,
        int requestTimeoutMs) {
}
