package vn.gastroai.be.api.notification;

import java.time.Instant;

/** UC0016 - ket qua sau khi bam nut "da uong" cho 1 reminder. */
public record MedicationConfirmationResponse(
        Long id,
        Long reminderId,
        Instant confirmedAt
) {
}
