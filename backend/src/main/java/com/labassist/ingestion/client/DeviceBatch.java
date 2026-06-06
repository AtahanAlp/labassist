package com.labassist.ingestion.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * A batch returned by the device poll endpoint: the next cursor plus the raw
 * result nodes. Results are kept as {@link JsonNode} so malformed messages can be
 * inspected and rejected individually rather than failing the whole batch.
 */
public record DeviceBatch(long cursor, List<JsonNode> results) {
}
