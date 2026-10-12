package vn.gastroai.be.api.notification;

import java.time.Instant;
import java.time.LocalTime;

/** UC0016 - ket qua sau khi bam nut "da uong" cho 1 reminder. */
public record MedicationConfirmationResponse(
        Long id,
        Long reminderId,
        LocalTime scheduledTime,
        Instant confirmedAt
) {
}
