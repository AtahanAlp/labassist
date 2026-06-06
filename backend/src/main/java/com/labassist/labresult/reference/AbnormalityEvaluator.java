package com.labassist.labresult.reference;

import com.labassist.common.domain.Sex;
import com.labassist.labresult.domain.AnalyteFlag;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Classifies an analyte value against the canonical reference range.
 *
 * <p>This is the deterministic core of the "preliminary analysis": critical
 * thresholds take precedence over the normal range, so a value is flagged
 * CRITICAL before merely LOW/HIGH. The LLM narrative is built on top of these
 * flags — it never replaces them.
 */
@Component
public class AbnormalityEvaluator {

    private final ReferenceRangeCatalog catalog;

    public AbnormalityEvaluator(ReferenceRangeCatalog catalog) {
        this.catalog = catalog;
    }

    public AnalyteFlag evaluate(String code, BigDecimal value, Sex sex, Integer age) {
        if (value == null) {
            return AnalyteFlag.UNKNOWN;
        }
        Optional<ReferenceRange> match = catalog.find(code, sex, age);
        return match.map(range -> classify(range, value)).orElse(AnalyteFlag.UNKNOWN);
    }

    /**
     * Pure classification of a measured value against a known range. Critical
     * thresholds take precedence over the normal range. Side-effect free and
     * dependency-free, so it is unit-tested directly without any test doubles.
     */
    public static AnalyteFlag classify(ReferenceRange range, BigDecimal value) {
        if (range.getCriticalLow() != null && value.compareTo(range.getCriticalLow()) < 0) {
            return AnalyteFlag.CRITICAL_LOW;
        }
        if (range.getCriticalHigh() != null && value.compareTo(range.getCriticalHigh()) > 0) {
            return AnalyteFlag.CRITICAL_HIGH;
        }
        if (range.getLow() != null && value.compareTo(range.getLow()) < 0) {
            return AnalyteFlag.LOW;
        }
        if (range.getHigh() != null && value.compareTo(range.getHigh()) > 0) {
            return AnalyteFlag.HIGH;
        }
        return AnalyteFlag.NORMAL;
    }
}
