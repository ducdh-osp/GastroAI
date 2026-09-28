package vn.gastroai.be.api.patient;

import java.time.Instant;
import java.util.List;

/**
 * UC0009/UC0010 - response cua GET/PUT /api/v1/patient/medical-profile.
 * exists=false khi benh nhan chua khai bao ho so lan nao ca - cac field con lai se rong/null.
 */
public record MedicalProfileResponse(
        Long id,
        String medicalHistory,
        List<String> allergies,
        List<String> currentMedications,
        Instant updatedAt,
        boolean exists
) {
}
