package vn.gastroai.be.application.patient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.api.patient.MedicalProfileRequest;
import vn.gastroai.be.api.patient.MedicalProfileResponse;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.patient.MedicalProfile;
import vn.gastroai.be.infrastructure.persistence.postgres.MedicalProfileRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;

import java.util.Collections;
import java.util.List;

/**
 * UC0009 (khai bao) + UC0010 (cap nhat) - ca 2 UC dung chung 1 thao tac UPSERT vao cung
 * 1 ban ghi medical_profiles/1 benh nhan, khong tach rieng endpoint tao/sua.
 * Nhan thang MedicalProfileRequest (thay vi no ra tung tham so) vi ho so co toi 10 field -
 * no ra se lam chu ky method qua dai, kho doc.
 */
@Service
public class MedicalProfileService {

    private final MedicalProfileRepository medicalProfileRepository;
    private final PatientRepository patientRepository;
    private final ObjectMapper objectMapper;

    public MedicalProfileService(MedicalProfileRepository medicalProfileRepository,
                                  PatientRepository patientRepository,
                                  ObjectMapper objectMapper) {
        this.medicalProfileRepository = medicalProfileRepository;
        this.patientRepository = patientRepository;
        this.objectMapper = objectMapper;
    }

    /** Luon tra ve 200 (khong 404) - neu benh nhan chua khai bao thi tra ve gia tri rong,
     * de FE khong can xu ly rieng truong hop "chua co ho so". */
    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public MedicalProfileResponse getProfile(Long patientId) {
        return medicalProfileRepository.findByPatientId(patientId)
                .map(this::toResponse)
                .orElse(new MedicalProfileResponse(null, null, null, null, null, null,
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                        Collections.emptyList(), Collections.emptyList(), null, false));
    }

    @Transactional("postgresTransactionManager")
    public MedicalProfileResponse upsertProfile(Long patientId, MedicalProfileRequest request) {
        MedicalProfile profile = medicalProfileRepository.findByPatientId(patientId)
                .orElseGet(() -> {
                    Patient patient = patientRepository.findById(patientId)
                            .orElseThrow(() -> new IllegalArgumentException("Patient not found"));
                    MedicalProfile created = new MedicalProfile();
                    created.setPatient(patient);
                    return created;
                });

        profile.setDateOfBirth(request.dateOfBirth());
        profile.setGender(request.gender());
        profile.setHeightCm(request.heightCm());
        profile.setWeightKg(request.weightKg());
        profile.setMedicalHistory(request.medicalHistory());
        profile.setAllergies(toJson(request.allergies()));
        profile.setChronicConditions(toJson(request.chronicConditions()));
        profile.setPastSurgeries(toJson(request.pastSurgeries()));
        profile.setCurrentMedications(toJson(request.currentMedications()));
        profile.setDietaryRestrictions(toJson(request.dietaryRestrictions()));

        MedicalProfile saved = medicalProfileRepository.save(profile);
        return toResponse(saved);
    }

    private MedicalProfileResponse toResponse(MedicalProfile profile) {
        return new MedicalProfileResponse(
                profile.getId(),
                profile.getDateOfBirth(),
                profile.getGender(),
                profile.getHeightCm(),
                profile.getWeightKg(),
                profile.getMedicalHistory(),
                parseJson(profile.getAllergies()),
                parseJson(profile.getChronicConditions()),
                parseJson(profile.getPastSurgeries()),
                parseJson(profile.getCurrentMedications()),
                parseJson(profile.getDietaryRestrictions()),
                profile.getUpdatedAt(),
                true);
    }

    private String toJson(List<String> value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private List<String> parseJson(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
