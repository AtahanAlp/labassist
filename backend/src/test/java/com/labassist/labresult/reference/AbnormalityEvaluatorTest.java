package com.labassist.labresult.reference;

import static org.assertj.core.api.Assertions.assertThat;

import com.labassist.common.domain.Sex;
import com.labassist.labresult.domain.AnalyteFlag;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Pure-logic tests for analyte classification — no mocking framework involved.
 * The threshold rules are exercised directly via {@link AbnormalityEvaluator#classify}.
 */
class AbnormalityEvaluatorTest {

    private ReferenceRange potassiumRange() {
        ReferenceRange range = new ReferenceRange();
        range.setCode("K");
        range.setLow(new BigDecimal("3.5"));
        range.setHigh(new BigDecimal("5.1"));
        range.setCriticalLow(new BigDecimal("2.5"));
        range.setCriticalHigh(new BigDecimal("6.5"));
        return range;
    }

    @Test
    void valueWithinRangeIsNormal() {
        assertThat(AbnormalityEvaluator.classify(potassiumRange(), new BigDecimal("4.2")))
                .isEqualTo(AnalyteFlag.NORMAL);
    }

    @Test
    void valueBelowLowButAboveCriticalIsLow() {
        assertThat(AbnormalityEvaluator.classify(potassiumRange(), new BigDecimal("3.0")))
                .isEqualTo(AnalyteFlag.LOW);
    }

    @Test
    void valueAboveHighButBelowCriticalIsHigh() {
        assertThat(AbnormalityEvaluator.classify(potassiumRange(), new BigDecimal("5.8")))
                .isEqualTo(AnalyteFlag.HIGH);
    }

    @Test
    void criticalThresholdsTakePrecedenceOverNormalRange() {
        assertThat(AbnormalityEvaluator.classify(potassiumRange(), new BigDecimal("7.2")))
                .isEqualTo(AnalyteFlag.CRITICAL_HIGH);
        assertThat(AbnormalityEvaluator.classify(potassiumRange(), new BigDecimal("2.0")))
                .isEqualTo(AnalyteFlag.CRITICAL_LOW);
    }

    @Test
    void rangeWithoutCriticalBoundsNeverFlagsCritical() {
        ReferenceRange crp = new ReferenceRange();
        crp.setCode("CRP");
        crp.setLow(BigDecimal.ZERO);
        crp.setHigh(new BigDecimal("5"));
        assertThat(AbnormalityEvaluator.classify(crp, new BigDecimal("999"))).isEqualTo(AnalyteFlag.HIGH);
    }

    @Test
    void nullValueIsUnknownWithoutConsultingCatalog() {
        // A null value short-circuits before the catalog is touched, so a null catalog is fine.
        AbnormalityEvaluator evaluator = new AbnormalityEvaluator(null);
        assertThat(evaluator.evaluate("K", null, Sex.M, 40)).isEqualTo(AnalyteFlag.UNKNOWN);
    }
}
