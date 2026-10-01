package vn.gastroai.be.api.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** UC0015 - body cua POST/PUT /api/v1/patient/medications. FE gui lai toan bo object ke
 * ca `active` khi sua (khong co endpoint PATCH rieng cho toggle). */
public record MedicationReminderRequest(
        @NotBlank @Size(max = 200, message = "ten thuoc toi da 200 ky tu") String medicineName,
        @Size(max = 200, message = "lieu luong toi da 200 ky tu") String dosage,
        @NotNull @Size(min = 1, max = 8, message = "moi thuoc can tu 1 den 8 gio nhac") List<@NotNull LocalTime> timesOfDay,
        LocalDate startDate,
        LocalDate endDate,
        @Size(max = 1000, message = "huong dan toi da 1000 ky tu") String instructions,
        boolean active
) {
    public MedicationReminderRequest(String medicineName, String dosage, LocalTime timeOfDay, boolean active) {
        this(medicineName, dosage, List.of(timeOfDay), null, null, null, active);
    }
}
