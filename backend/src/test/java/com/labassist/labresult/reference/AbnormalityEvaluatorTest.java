package com.labassist.labresult.reference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.labassist.common.domain.Sex;
import com.labassist.labresult.domain.AnalyteFlag;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AbnormalityEvaluatorTest {

    @Mock
    private ReferenceRangeCatalog catalog;

    private AbnormalityEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new AbnormalityEvaluator(catalog);
    }

    private ReferenceRange potassiumRange() {
        ReferenceRange range = new ReferenceRange();
        range.setCode("K");
        range.setLow(new BigDecimal("3.5"));
        range.setHigh(new BigDecimal("5.1"));
        range.setCriticalLow(new BigDecimal("2.5"));
        range.setCriticalHigh(new BigDecimal("6.5"));
        return range;
    }

    private void stubRange(ReferenceRange range) {
        when(catalog.find(any(), any(), any())).thenReturn(Optional.ofNullable(range));
    }

    @Test
    void valueWithinRangeIsNormal() {
        stubRange(potassiumRange());
        assertThat(evaluator.evaluate("K", new BigDecimal("4.2"), Sex.M, 40)).isEqualTo(AnalyteFlag.NORMAL);
    }

    @Test
    void valueBelowLowButAboveCriticalIsLow() {
        stubRange(potassiumRange());
        assertThat(evaluator.evaluate("K", new BigDecimal("3.0"), Sex.M, 40)).isEqualTo(AnalyteFlag.LOW);
    }

    @Test
    void valueAboveHighButBelowCriticalIsHigh() {
        stubRange(potassiumRange());
        assertThat(evaluator.evaluate("K", new BigDecimal("5.8"), Sex.M, 40)).isEqualTo(AnalyteFlag.HIGH);
    }

    @Test
    void criticalThresholdsTakePrecedenceOverNormalRange() {
        stubRange(potassiumRange());
        assertThat(evaluator.evaluate("K", new BigDecimal("7.2"), Sex.M, 40)).isEqualTo(AnalyteFlag.CRITICAL_HIGH);
        assertThat(evaluator.evaluate("K", new BigDecimal("2.0"), Sex.M, 40)).isEqualTo(AnalyteFlag.CRITICAL_LOW);
    }

    @Test
    void nullValueIsUnknown() {
        assertThat(evaluator.evaluate("K", null, Sex.M, 40)).isEqualTo(AnalyteFlag.UNKNOWN);
    }

    @Test
    void missingReferenceRangeIsUnknown() {
        stubRange(null);
        assertThat(evaluator.evaluate("ZZZ", new BigDecimal("1"), Sex.M, 40)).isEqualTo(AnalyteFlag.UNKNOWN);
    }

    @Test
    void rangeWithoutCriticalBoundsNeverFlagsCritical() {
        ReferenceRange crp = new ReferenceRange();
        crp.setCode("CRP");
        crp.setLow(BigDecimal.ZERO);
        crp.setHigh(new BigDecimal("5"));
        stubRange(crp);
        assertThat(evaluator.evaluate("CRP", new BigDecimal("999"), Sex.M, 40)).isEqualTo(AnalyteFlag.HIGH);
    }
}
