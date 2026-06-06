package com.labassist.ingestion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.labassist.common.domain.Sex;
import com.labassist.ingestion.client.DeviceMessageDto;
import com.labassist.ingestion.client.DeviceTestDto;
import com.labassist.labresult.LabReportRepository;
import com.labassist.labresult.domain.AnalyteFlag;
import com.labassist.labresult.domain.LabReport;
import com.labassist.labresult.domain.ReportStatus;
import com.labassist.labresult.domain.TestResult;
import com.labassist.labresult.reference.AbnormalityEvaluator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Validates, maps, flags and persists a single device message in its own
 * transaction, so one bad message never aborts the rest of a batch.
 *
 * <p>Flow: parse → bean-validate → idempotency check → map + abnormality flagging
 * → persist. Unparseable or invalid messages are stored as REJECTED with the raw
 * payload and the reason, demonstrating that validation caught them.
 */
@Slf4j
@Component
public class MessageIngestor {

    private static final int MAX_REASON_LENGTH = 512;

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final LabReportRepository reportRepository;
    private final AbnormalityEvaluator abnormalityEvaluator;

    public MessageIngestor(ObjectMapper objectMapper, Validator validator,
                           LabReportRepository reportRepository, AbnormalityEvaluator abnormalityEvaluator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.reportRepository = reportRepository;
        this.abnormalityEvaluator = abnormalityEvaluator;
    }

    @Transactional
    public IngestionOutcome ingest(JsonNode node) {
        DeviceMessageDto message;
        try {
            message = objectMapper.treeToValue(node, DeviceMessageDto.class);
        } catch (Exception e) {
            return reject(node, "Unparseable payload: " + rootMessage(e));
        }

        Set<ConstraintViolation<DeviceMessageDto>> violations = validator.validate(message);
        if (!violations.isEmpty()) {
            String reason = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining("; "));
            return reject(node, "Validation failed: " + reason);
        }

        if (reportRepository.existsByExternalId(message.externalId())) {
            return IngestionOutcome.skipped();
        }

        LabReport report = mapToReport(message, redactPii(node));
        Sex sex = report.getPatientSex();
        Integer age = report.getPatientAge();

        boolean partial = false;
        int abnormal = 0;
        int critical = 0;
        for (DeviceTestDto test : message.tests()) {
            AnalyteFlag flag = abnormalityEvaluator.evaluate(test.code(), test.value(), sex, age);
            TestResult result = new TestResult();
            result.setCode(test.code());
            result.setName(test.name());
            result.setValue(test.value());
            result.setUnit(test.unit());
            result.setRefLow(test.refLow());
            result.setRefHigh(test.refHigh());
            result.setFlag(flag);
            report.addTest(result);

            if (test.value() == null) {
                partial = true;
            }
            if (flag.isAbnormal()) {
                abnormal++;
            }
            if (flag.isCritical()) {
                critical++;
            }
        }

        report.setStatus(partial ? ReportStatus.PARTIAL : ReportStatus.VALIDATED);
        report.setAbnormalCount(abnormal);
        report.setCriticalCount(critical);
        report.setOverallAbnormal(abnormal > 0);
        reportRepository.save(report);
        return IngestionOutcome.stored(abnormal > 0, critical > 0, partial);
    }

    private LabReport mapToReport(DeviceMessageDto message, String rawPayload) {
        LabReport report = new LabReport();
        report.setExternalId(message.externalId());
        report.setDeviceId(message.deviceId());
        report.setPatientName(message.patient().name());
        report.setPatientMrn(message.patient().mrn());
        report.setPatientAge(message.patient().age());
        report.setPatientSex(parseSex(message.patient().sex()));
        report.setSampleCollectedAt(message.sampleCollectedAt());
        report.setReceivedAt(Instant.now());
        report.setRawPayload(rawPayload);
        return report;
    }

    private IngestionOutcome reject(JsonNode node, String reason) {
        String externalId = node.path("externalId").isTextual() ? node.get("externalId").asText() : null;
        if (externalId != null && reportRepository.existsByExternalId(externalId)) {
            return IngestionOutcome.skipped();
        }
        LabReport report = new LabReport();
        report.setExternalId(externalId != null ? externalId : "MALFORMED-" + UUID.randomUUID());
        report.setStatus(ReportStatus.REJECTED);
        report.setRejectionReason(truncate(reason));
        report.setRawPayload(redactPii(node));
        report.setReceivedAt(Instant.now());
        report.setOverallAbnormal(false);
        reportRepository.save(report);
        log.debug("Rejected message externalId={} reason={}", externalId, reason);
        return IngestionOutcome.rejected();
    }

    /**
     * Serializes the raw device payload with patient identifiers stripped.
     *
     * <p>The raw payload is retained for debugging/traceability, but name and MRN
     * are the protected fields — they live encrypted in their own columns, so we
     * never persist them in plaintext here.
     */
    private static String redactPii(JsonNode node) {
        JsonNode copy = node.deepCopy();
        if (copy.isObject() && copy.path("patient").isObject()) {
            ObjectNode patient = (ObjectNode) copy.get("patient");
            if (patient.has("name")) {
                patient.put("name", "[REDACTED]");
            }
            if (patient.has("mrn")) {
                patient.put("mrn", "[REDACTED]");
            }
        }
        return copy.toString();
    }

    private static String rootMessage(Throwable e) {
        String message = e.getMessage();
        return message == null ? e.getClass().getSimpleName() : message;
    }

    private static Sex parseSex(String raw) {
        if (raw == null) {
            return Sex.UNKNOWN;
        }
        return switch (raw.trim().toUpperCase()) {
            case "M" -> Sex.M;
            case "F" -> Sex.F;
            default -> Sex.UNKNOWN;
        };
    }

    private static String truncate(String value) {
        return value.length() <= MAX_REASON_LENGTH ? value : value.substring(0, MAX_REASON_LENGTH);
    }
}
