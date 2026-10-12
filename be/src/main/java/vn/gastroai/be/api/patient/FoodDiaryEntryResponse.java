package vn.gastroai.be.api.patient;

import java.time.Instant;
import vn.gastroai.be.domain.patient.MealType;

/** UC0011 - 1 muc trong nhat ky an uong. */
public record FoodDiaryEntryResponse(
        Long id,
        Instant eatenAt,
        String description,
        MealType mealType,
        String symptomsAfterMeal,
        Integer symptomOnsetMinutes,
        String notes
) {
}
