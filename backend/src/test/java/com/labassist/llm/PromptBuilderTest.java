package com.labassist.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.labassist.common.domain.Sex;
import com.labassist.labresult.domain.AnalyteFlag;
import com.labassist.labresult.domain.LabReport;
import com.labassist.labresult.domain.TestResult;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PromptBuilderTest {

    private final PromptBuilder promptBuilder = new PromptBuilder();

    private LabReport sampleReport() {
        LabReport report = new LabReport();
        report.setPatientName("Top Secret Name");
        report.setPatientMrn("MRN-SECRET-999");
        report.setPatientAge(64);
        report.setPatientSex(Sex.F);

        TestResult potassium = new TestResult();
        potassium.setCode("K");
        potassium.setName("Potassium");
        potassium.setValue(new BigDecimal("6.9"));
        potassium.setUnit("mmol/L");
        potassium.setRefLow(new BigDecimal("3.5"));
        potassium.setRefHigh(new BigDecimal("5.1"));
        potassium.setFlag(AnalyteFlag.CRITICAL_HIGH);
        report.addTest(potassium);
        return report;
    }

    @Test
    void userPromptIncludesDeidentifiedClinicalData() {
        String prompt = promptBuilder.buildUserPrompt(sampleReport());

        assertThat(prompt).contains("64").contains("kadın"); // sex F rendered in Turkish
        assertThat(prompt).contains("Potassium").contains("6.9").contains("Kritik Yüksek");
    }

    @Test
    void userPromptNeverLeaksPatientIdentifiers() {
        String prompt = promptBuilder.buildUserPrompt(sampleReport());

        assertThat(prompt).doesNotContain("Top Secret Name");
        assertThat(prompt).doesNotContain("MRN-SECRET-999");
    }

    @Test
    void systemPromptEnforcesNonDiagnosticTurkishFraming() {
        assertThat(promptBuilder.buildSystemPrompt())
                .containsIgnoringCase("tanı")   // non-diagnostic framing
                .containsIgnoringCase("hekim")  // physician review
                .containsIgnoringCase("TÜRKÇE");
    }
}
