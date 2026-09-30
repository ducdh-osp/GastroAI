package vn.gastroai.be.api.patient;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** UC0011 - body cua POST/PUT /api/v1/patient/food-diary. */
public record FoodDiaryEntryRequest(
        @NotNull @PastOrPresent(message = "thoi diem an khong duoc o tuong lai") Instant eatenAt,
        @NotBlank @Size(max = 500, message = "mon an toi da 500 ky tu") String description,
        @Size(max = 5000, message = "ghi chu toi da 5000 ky tu") String notes
) {
}
