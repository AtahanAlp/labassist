package com.labassist.ingestion.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

/** A single analyte measurement in a device message. A null {@code value} is allowed (partial). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DeviceTestDto(
        @NotBlank String code,
        String name,
        BigDecimal value,
        String unit,
        BigDecimal refLow,
        BigDecimal refHigh) {
}
