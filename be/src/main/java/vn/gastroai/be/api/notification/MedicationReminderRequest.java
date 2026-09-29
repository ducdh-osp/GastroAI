package vn.gastroai.be.api.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/** UC0015 - body cua POST/PUT /api/v1/patient/medications. FE gui lai toan bo object ke
 * ca `active` khi sua (khong co endpoint PATCH rieng cho toggle). */
public record MedicationReminderRequest(
        @NotBlank String medicineName,
        String dosage,
        @NotNull LocalTime timeOfDay,
        boolean active
) {
}
