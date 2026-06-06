package com.labassist.ingestion;

import java.util.LinkedHashMap;
import java.util.Map;

/** Aggregate counts for one poll cycle, used for logging and audit. */
public record IngestionSummary(
        int fetched,
        int stored,
        int skippedDuplicates,
        int rejected,
        int partial,
        int abnormalReports,
        int criticalReports) {

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("fetched", fetched);
        map.put("stored", stored);
        map.put("skippedDuplicates", skippedDuplicates);
        map.put("rejected", rejected);
        map.put("partial", partial);
        map.put("abnormalReports", abnormalReports);
        map.put("criticalReports", criticalReports);
        return map;
    }
}
