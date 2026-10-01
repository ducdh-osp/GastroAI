package vn.gastroai.be.api.patient;

import java.time.Instant;
import vn.gastroai.be.domain.patient.MealType;

public record DigestiveTimelineItem(
        String type,
        Long id,
        Instant occurredAt,
        MealType mealType,
        String description,
        String symptomsAfterMeal,
        Integer symptomOnsetMinutes,
        Integer bristolType,
        String notes
) {
}
