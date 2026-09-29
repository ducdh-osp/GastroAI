package vn.gastroai.be.api.patient;

import java.time.Instant;

/** UC0011 - 1 muc trong nhat ky an uong. */
public record FoodDiaryEntryResponse(
        Long id,
        Instant eatenAt,
        String description,
        String notes
) {
}
