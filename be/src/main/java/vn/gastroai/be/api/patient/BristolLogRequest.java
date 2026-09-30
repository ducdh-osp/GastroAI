package vn.gastroai.be.api.patient;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** UC0013 - body cua POST/PUT /api/v1/patient/bristol-logs. */
public record BristolLogRequest(
        @NotNull @PastOrPresent(message = "thoi diem khong duoc o tuong lai") Instant loggedAt,
        @NotNull @Min(1) @Max(7) Integer bristolType,
        @Size(max = 5000, message = "ghi chu toi da 5000 ky tu") String notes
) {
}
