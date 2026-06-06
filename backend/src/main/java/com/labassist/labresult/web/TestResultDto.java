package com.labassist.labresult.web;

import com.labassist.labresult.domain.AnalyteFlag;
import java.math.BigDecimal;

/** A single analyte result for the API, including its computed flag. */
public record TestResultDto(
        String code,
        String name,
        BigDecimal value,
        String unit,
        BigDecimal refLow,
        BigDecimal refHigh,
        AnalyteFlag flag) {
}
