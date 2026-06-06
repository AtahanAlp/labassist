package com.labassist.llm;

import com.labassist.labresult.domain.LabReport;
import com.labassist.labresult.domain.TestResult;
import java.util.Comparator;
import org.springframework.stereotype.Component;

/**
 * Builds the LLM prompt from a report.
 *
 * <p>Privacy by design: the prompt contains only de-identified data — patient age
 * and sex plus the analyte values, reference ranges and computed flags. Patient
 * name and MRN are never sent to the LLM. The flags come from the deterministic
 * {@code AbnormalityEvaluator}, so the model narrates a pre-computed analysis
 * rather than deciding what is abnormal.
 */
@Component
public class PromptBuilder {

    public static final String PROMPT_VERSION = "v1";

    private static final String SYSTEM_PROMPT = """
            You are a clinical laboratory assistant that writes a PRELIMINARY, NON-DIAGNOSTIC
            interpretation of lab results to support a physician's review.

            Rules:
            - Do NOT give a definitive diagnosis or prescribe treatment.
            - Address the CRITICAL values first, then other abnormal values.
            - For each abnormal value, briefly note what it may indicate (differential considerations).
            - Be concise and use short bullet points.
            - You receive only de-identified data (age, sex, analyte values, reference ranges, flags).
            - Always end with a one-line reminder to correlate with the clinical picture and that a
              physician must review and confirm.
            """;

    public String buildSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String buildUserPrompt(LabReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("Patient: age ")
                .append(report.getPatientAge() == null ? "unknown" : report.getPatientAge())
                .append(", sex ")
                .append(report.getPatientSex() == null ? "unknown" : report.getPatientSex())
                .append(".\n\n");
        sb.append("Lab panel (value [flag], reference range):\n");

        report.getTests().stream()
                .sorted(Comparator.comparing(TestResult::getCode))
                .forEach(test -> appendTest(sb, test));

        sb.append("\nWrite a brief preliminary interpretation focused on the abnormal and critical values.");
        return sb.toString();
    }

    private void appendTest(StringBuilder sb, TestResult test) {
        sb.append("- ")
                .append(test.getName() != null ? test.getName() : test.getCode())
                .append(" (").append(test.getCode()).append("): ")
                .append(test.getValue() == null ? "not measured" : test.getValue().toPlainString());
        if (test.getUnit() != null) {
            sb.append(' ').append(test.getUnit());
        }
        sb.append(" [").append(test.getFlag()).append(']');
        if (test.getRefLow() != null || test.getRefHigh() != null) {
            sb.append(", ref ")
                    .append(test.getRefLow() != null ? test.getRefLow().toPlainString() : "")
                    .append('-')
                    .append(test.getRefHigh() != null ? test.getRefHigh().toPlainString() : "");
        }
        sb.append('\n');
    }
}
