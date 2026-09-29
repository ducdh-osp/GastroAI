package vn.gastroai.be.api.patient;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/** UC0013 - body cua POST/PUT /api/v1/patient/bristol-logs. */
public record BristolLogRequest(
        @NotNull Instant loggedAt,
        @NotNull @Min(1) @Max(7) Integer bristolType,
        String notes
) {
}
