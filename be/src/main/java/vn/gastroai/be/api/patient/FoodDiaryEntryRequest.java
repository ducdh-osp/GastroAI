package vn.gastroai.be.api.patient;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/** UC0011 - body cua POST/PUT /api/v1/patient/food-diary. */
public record FoodDiaryEntryRequest(
        @NotNull Instant eatenAt,
        @NotBlank String description,
        String notes
) {
}
