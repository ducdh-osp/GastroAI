package vn.gastroai.be.api.notification;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/** UC0015 - 1 lich nhac uong thuoc. */
public record MedicationReminderResponse(
        Long id,
        Long version,
        String medicineName,
        String dosage,
        List<LocalTime> timesOfDay,
        LocalDate startDate,
        LocalDate endDate,
        String instructions,
        boolean active,
        Set<LocalTime> confirmedTimesToday
) {
}
