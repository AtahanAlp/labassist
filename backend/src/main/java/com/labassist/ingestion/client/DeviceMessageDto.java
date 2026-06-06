package com.labassist.ingestion.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

/**
 * A lab report message as emitted by the device. Bean-validation constraints
 * define a well-formed message; violations cause the report to be REJECTED.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DeviceMessageDto(
        @NotBlank String externalId,
        String deviceId,
        @NotNull @Valid DevicePatientDto patient,
        @NotNull Instant sampleCollectedAt,
        @NotEmpty @Valid List<DeviceTestDto> tests) {
}
