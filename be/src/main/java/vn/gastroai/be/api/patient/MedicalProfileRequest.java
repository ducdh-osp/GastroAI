package vn.gastroai.be.api.patient;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** UC0009/UC0010 - body cua PUT /api/v1/patient/medical-profile. Tat ca field duoc phep
 * de trong/null (benh nhan co the chua khai bao het). currentMedications la danh sach
 * text tu khai cho thuoc khong can lich nhac trong app; lich thuoc co gio/dose dung
 * MedicationReminder. Hai danh sach khong tu dong dong bo. */
public record MedicalProfileRequest(
        // null only when creating a profile for the first time
        Long version,
        @Past(message = "ngay sinh phai o qua khu")
        LocalDate dateOfBirth,
        @Pattern(regexp = "MALE|FEMALE|OTHER", message = "gender phai la MALE, FEMALE hoac OTHER")
        String gender,
        @Min(value = 30, message = "chieu cao toi thieu 30cm")
        @Max(value = 300, message = "chieu cao toi da 300cm")
        Integer heightCm,
        @Min(value = 1, message = "can nang toi thieu 1kg")
        @Max(value = 500, message = "can nang toi da 500kg")
        Integer weightKg,
        @Size(max = 5000, message = "ghi chu tien su toi da 5000 ky tu") String medicalHistory,
        List<@Size(max = 200, message = "moi muc toi da 200 ky tu") String> allergies,
        List<@Size(max = 200, message = "moi muc toi da 200 ky tu") String> chronicConditions,
        List<@Size(max = 200, message = "moi muc toi da 200 ky tu") String> pastSurgeries,
        List<@Size(max = 200, message = "moi muc toi da 200 ky tu") String> currentMedications,
        List<@Size(max = 200, message = "moi muc toi da 200 ky tu") String> dietaryRestrictions
) {
}
