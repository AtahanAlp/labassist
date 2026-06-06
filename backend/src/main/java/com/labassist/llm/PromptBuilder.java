package com.labassist.llm;

import com.labassist.common.domain.Sex;
import com.labassist.labresult.domain.AnalyteFlag;
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
 *
 * <p>The assistant is instructed to answer in Turkish, matching the doctor-facing UI.
 */
@Component
public class PromptBuilder {

    public static final String PROMPT_VERSION = "v2-tr";

    private static final String SYSTEM_PROMPT = """
            Sen, bir hekimin değerlendirmesini desteklemek için laboratuvar sonuçlarının ÖN
            ve TANI NİTELİĞİ TAŞIMAYAN bir yorumunu yazan klinik bir laboratuvar asistanısın.

            Kurallar:
            - Kesin tanı koyma veya tedavi önerme.
            - Önce KRİTİK değerleri, ardından diğer anormal değerleri ele al.
            - Her anormal değer için ne anlama gelebileceğini kısaca belirt (olası ayırıcı tanılar).
            - Kısa tut ve madde işaretleri kullan.
            - Sana yalnızca kimliksizleştirilmiş veriler verilir (yaş, cinsiyet, analit değerleri,
              referans aralıkları ve bayraklar).
            - Yanıtının tamamını TÜRKÇE yaz.
            - Sonunda, bulguların hastanın klinik tablosuyla birlikte değerlendirilmesi ve bir hekim
              tarafından gözden geçirilip onaylanması gerektiğini tek cümleyle hatırlat.
            """;

    public String buildSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String buildUserPrompt(LabReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hasta: yaş ")
                .append(report.getPatientAge() == null ? "bilinmiyor" : report.getPatientAge())
                .append(", cinsiyet ")
                .append(sexLabel(report.getPatientSex()))
                .append(".\n\n");
        sb.append("Lab paneli (değer [değerlendirme], referans aralığı):\n");

        report.getTests().stream()
                .sorted(Comparator.comparing(TestResult::getCode))
                .forEach(test -> appendTest(sb, test));

        sb.append("\nAnormal ve kritik değerlere odaklanan kısa bir ön değerlendirme yaz.");
        return sb.toString();
    }

    private void appendTest(StringBuilder sb, TestResult test) {
        sb.append("- ")
                .append(test.getName() != null ? test.getName() : test.getCode())
                .append(" (").append(test.getCode()).append("): ")
                .append(test.getValue() == null ? "ölçülmedi" : test.getValue().toPlainString());
        if (test.getUnit() != null) {
            sb.append(' ').append(test.getUnit());
        }
        sb.append(" [").append(flagLabel(test.getFlag())).append(']');
        if (test.getRefLow() != null || test.getRefHigh() != null) {
            sb.append(", referans ")
                    .append(test.getRefLow() != null ? test.getRefLow().toPlainString() : "")
                    .append('-')
                    .append(test.getRefHigh() != null ? test.getRefHigh().toPlainString() : "");
        }
        sb.append('\n');
    }

    private static String sexLabel(Sex sex) {
        if (sex == null) {
            return "bilinmiyor";
        }
        return switch (sex) {
            case M -> "erkek";
            case F -> "kadın";
            case UNKNOWN -> "bilinmiyor";
        };
    }

    private static String flagLabel(AnalyteFlag flag) {
        return switch (flag) {
            case NORMAL -> "Normal";
            case LOW -> "Düşük";
            case HIGH -> "Yüksek";
            case CRITICAL_LOW -> "Kritik Düşük";
            case CRITICAL_HIGH -> "Kritik Yüksek";
            case UNKNOWN -> "Bilinmiyor";
        };
    }
}
