package vn.gastroai.be.api.patient;

import java.util.List;

/** UC0009/UC0010 - body cua PUT /api/v1/patient/medical-profile. Tat ca field duoc phep
 * de trong/null (benh nhan co the chua khai bao het). */
public record MedicalProfileRequest(
        String medicalHistory,
        List<String> allergies,
        List<String> currentMedications
) {
}
