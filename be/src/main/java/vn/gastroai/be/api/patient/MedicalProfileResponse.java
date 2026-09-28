package vn.gastroai.be.api.patient;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * UC0009/UC0010 - response cua GET/PUT /api/v1/patient/medical-profile.
 * exists=false khi benh nhan chua khai bao ho so lan nao ca - cac field con lai se rong/null.
 */
public record MedicalProfileResponse(
        Long id,
        LocalDate dateOfBirth,
        String gender,
        Integer heightCm,
        Integer weightKg,
        String medicalHistory,
        List<String> allergies,
        List<String> chronicConditions,
        List<String> pastSurgeries,
        List<String> currentMedications,
        List<String> dietaryRestrictions,
        Instant updatedAt,
        boolean exists
) {
}
