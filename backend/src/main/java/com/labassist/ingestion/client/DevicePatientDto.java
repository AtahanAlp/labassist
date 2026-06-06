package com.labassist.ingestion.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Patient block of a device message. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DevicePatientDto(
        @NotBlank String name,
        @NotBlank String mrn,
        @NotNull @Min(0) @Max(150) Integer age,
        String sex) {
}
