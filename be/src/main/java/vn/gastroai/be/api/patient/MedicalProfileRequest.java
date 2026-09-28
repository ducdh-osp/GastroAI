package vn.gastroai.be.api.patient;

import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.List;

/** UC0009/UC0010 - body cua PUT /api/v1/patient/medical-profile. Tat ca field duoc phep
 * de trong/null (benh nhan co the chua khai bao het). */
public record MedicalProfileRequest(
        LocalDate dateOfBirth,
        @Pattern(regexp = "MALE|FEMALE|OTHER", message = "gender phai la MALE, FEMALE hoac OTHER")
        String gender,
        Integer heightCm,
        Integer weightKg,
        String medicalHistory,
        List<String> allergies,
        List<String> chronicConditions,
        List<String> pastSurgeries,
        List<String> currentMedications,
        List<String> dietaryRestrictions
) {
}
