package com.labassist.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labassist.TestcontainersConfiguration;
import com.labassist.ingestion.IngestionOutcome.Type;
import com.labassist.labresult.LabReportRepository;
import com.labassist.labresult.domain.AnalyteFlag;
import com.labassist.labresult.domain.LabReport;
import com.labassist.labresult.domain.ReportStatus;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Exercises the full ingestion path (validate → flag → encrypt → persist) against
 * a real PostgreSQL via Testcontainers, with the live reference-range seed.
 */
@SpringBootTest(properties = "labassist.lab-device.polling-enabled=false")
@Import(TestcontainersConfiguration.class)
class IngestionIntegrationTest {

    @Autowired
    private MessageIngestor messageIngestor;
    @Autowired
    private LabReportRepository reportRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clean() {
        reportRepository.deleteAll();
    }

    /** Re-fetch the single stored report with its analytes eagerly loaded. */
    private LabReport firstReportWithTests() {
        return reportRepository.findWithTestsById(reportRepository.findAll().get(0).getId()).orElseThrow();
    }

    private JsonNode message(String externalId, String name, String sex, List<Map<String, Object>> tests) {
        Map<String, Object> patient = new LinkedHashMap<>();
        patient.put("name", name);
        patient.put("mrn", "MRN-1001");
        patient.put("age", 55);
        patient.put("sex", sex);

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("externalId", externalId);
        message.put("deviceId", "ANALYZER-TEST");
        message.put("patient", patient);
        message.put("sampleCollectedAt", "2026-06-06T08:00:00Z");
        message.put("tests", tests);
        return objectMapper.valueToTree(message);
    }

    private Map<String, Object> test(String code, Object value) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("code", code);
        t.put("name", code);
        t.put("value", value);
        t.put("unit", "u");
        return t;
    }

    @Test
    void normalMessageIsStoredAndValidated() {
        IngestionOutcome outcome = messageIngestor.ingest(message("N-1", "Normal Patient", "M", List.of(test("K", 4.2))));

        assertThat(outcome.type()).isEqualTo(Type.STORED);
        LabReport report = firstReportWithTests();
        assertThat(report.getStatus()).isEqualTo(ReportStatus.VALIDATED);
        assertThat(report.isOverallAbnormal()).isFalse();
        assertThat(report.getTests().get(0).getFlag()).isEqualTo(AnalyteFlag.NORMAL);
    }

    @Test
    void criticalValueIsFlaggedCritical() {
        messageIngestor.ingest(message("C-1", "Critical Patient", "M", List.of(test("K", 7.2))));

        LabReport report = firstReportWithTests();
        assertThat(report.getCriticalCount()).isEqualTo(1);
        assertThat(report.isOverallAbnormal()).isTrue();
        assertThat(report.getTests().get(0).getFlag()).isEqualTo(AnalyteFlag.CRITICAL_HIGH);
    }

    @Test
    void missingValueMakesReportPartial() {
        List<Map<String, Object>> tests = new ArrayList<>();
        tests.add(test("K", 4.0));
        tests.add(test("GLU", null));
        messageIngestor.ingest(message("P-1", "Partial Patient", "F", tests));

        LabReport report = reportRepository.findAll().get(0);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.PARTIAL);
    }

    @Test
    void malformedMessageIsRejected() {
        JsonNode malformed = message("X-1", "Bad", "M", List.of(test("K", 4.0)));
        ((com.fasterxml.jackson.databind.node.ObjectNode) malformed).remove("externalId");

        IngestionOutcome outcome = messageIngestor.ingest(malformed);

        assertThat(outcome.type()).isEqualTo(Type.REJECTED);
        LabReport report = reportRepository.findAll().get(0);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.REJECTED);
        assertThat(report.getRejectionReason()).isNotBlank();
    }

    @Test
    void duplicateExternalIdIsSkipped() {
        JsonNode msg = message("DUP-1", "Dup Patient", "M", List.of(test("K", 4.0)));
        assertThat(messageIngestor.ingest(msg).type()).isEqualTo(Type.STORED);
        assertThat(messageIngestor.ingest(msg).type()).isEqualTo(Type.SKIPPED_DUPLICATE);
        assertThat(reportRepository.count()).isEqualTo(1);
    }

    @Test
    void piiIsEncryptedAtRestAndRedactedFromRawPayload() {
        messageIngestor.ingest(message("E-1", "Atahan Yilmaz", "F", List.of(test("K", 4.0))));

        // Encrypted column holds ciphertext; the entity getter decrypts it.
        String encryptedName = jdbcTemplate.queryForObject(
                "select patient_name from lab_report where external_id = 'E-1'", String.class);
        assertThat(encryptedName).isNotNull().isNotEqualTo("Atahan Yilmaz");
        assertThat(reportRepository.findAll().get(0).getPatientName()).isEqualTo("Atahan Yilmaz");

        // Raw payload keeps the structure but not the patient identifiers.
        String rawPayload = jdbcTemplate.queryForObject(
                "select raw_payload::text from lab_report where external_id = 'E-1'", String.class);
        assertThat(rawPayload).doesNotContain("Atahan Yilmaz").doesNotContain("MRN-1001").contains("[REDACTED]");
    }
}
