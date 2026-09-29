package vn.gastroai.be.api.notification;

import java.time.LocalTime;

/** UC0015 - 1 lich nhac uong thuoc. */
public record MedicationReminderResponse(
        Long id,
        String medicineName,
        String dosage,
        LocalTime timeOfDay,
        boolean active
) {
}
