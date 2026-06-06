package com.labassist.ingestion.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.labassist.config.LabDeviceProperties;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HTTP client for the external lab-device mock. Reads the poll endpoint as a raw
 * JSON tree so individual malformed messages can be handled downstream.
 */
@Component
public class LabDeviceClient {

    private final RestClient restClient;

    public LabDeviceClient(LabDeviceProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.requestTimeoutMs());
        factory.setReadTimeout(properties.requestTimeoutMs());
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .build();
    }

    /**
     * Fetches results newer than {@code cursor}. Propagates HTTP/transport errors
     * so the caller can leave the cursor unchanged and retry on the next cycle.
     */
    public DeviceBatch fetchSince(long cursor) {
        JsonNode body = restClient.get()
                .uri("/api/lab-results?since={cursor}", cursor)
                .retrieve()
                .body(JsonNode.class);
        if (body == null) {
            return new DeviceBatch(cursor, List.of());
        }
        long nextCursor = body.path("cursor").asLong(cursor);
        List<JsonNode> results = new ArrayList<>();
        JsonNode resultsNode = body.path("results");
        if (resultsNode.isArray()) {
            resultsNode.forEach(results::add);
        }
        return new DeviceBatch(nextCursor, results);
    }
}
