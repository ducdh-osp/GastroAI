package vn.gastroai.be.api.patient;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.List;

/** UC0009/UC0010 - body cua PUT /api/v1/patient/medical-profile. Tat ca field duoc phep
 * de trong/null (benh nhan co the chua khai bao het). */
public record MedicalProfileRequest(
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
        String medicalHistory,
        List<String> allergies,
        List<String> chronicConditions,
        List<String> pastSurgeries,
        List<String> currentMedications,
        List<String> dietaryRestrictions
) {
}
