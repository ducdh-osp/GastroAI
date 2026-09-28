package vn.gastroai.be.application.patient;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import vn.gastroai.be.api.patient.MedicalProfileRequest;
import vn.gastroai.be.api.patient.MedicalProfileResponse;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.patient.MedicalProfile;
import vn.gastroai.be.infrastructure.persistence.postgres.MedicalProfileRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MedicalProfileServiceTest {

    private final MedicalProfileRepository medicalProfileRepository = mock(MedicalProfileRepository.class);
    private final PatientRepository patientRepository = mock(PatientRepository.class);
    private final MedicalProfileService service =
            new MedicalProfileService(medicalProfileRepository, patientRepository, new ObjectMapper());

    private static MedicalProfileRequest sampleRequest(String medicalHistory) {
        return new MedicalProfileRequest(
                LocalDate.of(1995, 5, 20), "MALE", 172, 65,
                medicalHistory,
                List.of("Penicillin"), List.of("Viem dai trang man"), List.of("Cat ruot thua 2020"),
                List.of("Omeprazole 20mg"), List.of("Khong dung nap lactose"));
    }

    @Test
    void getProfileReturnsEmptyDefaultsWhenPatientHasNotDeclaredYet() {
        when(medicalProfileRepository.findByPatientId(1L)).thenReturn(Optional.empty());

        MedicalProfileResponse response = service.getProfile(1L);

        assertFalse(response.exists());
        assertTrue(response.allergies().isEmpty());
        assertTrue(response.chronicConditions().isEmpty());
        assertTrue(response.dietaryRestrictions().isEmpty());
    }

    @Test
    void upsertCreatesNewProfileWhenNoneExists() {
        Patient patient = new Patient();
        patient.setId(1L);
        when(medicalProfileRepository.findByPatientId(1L)).thenReturn(Optional.empty());
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(medicalProfileRepository.save(any(MedicalProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        MedicalProfileResponse response = service.upsertProfile(1L, sampleRequest("Tung phau thuat ruot thua nam 2020"));

        assertTrue(response.exists());
        assertEquals(LocalDate.of(1995, 5, 20), response.dateOfBirth());
        assertEquals("MALE", response.gender());
        assertEquals(172, response.heightCm());
        assertEquals(65, response.weightKg());
        assertEquals(List.of("Viem dai trang man"), response.chronicConditions());
        assertEquals(List.of("Cat ruot thua 2020"), response.pastSurgeries());
        assertEquals(List.of("Khong dung nap lactose"), response.dietaryRestrictions());
    }

    @Test
    void upsertUpdatesExistingProfileInsteadOfCreatingDuplicate() {
        Patient patient = new Patient();
        patient.setId(1L);
        MedicalProfile existing = new MedicalProfile();
        existing.setPatient(patient);
        existing.setMedicalHistory("Cu");
        when(medicalProfileRepository.findByPatientId(1L)).thenReturn(Optional.of(existing));
        when(medicalProfileRepository.save(any(MedicalProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        MedicalProfileResponse response = service.upsertProfile(1L, sampleRequest("Moi"));

        assertEquals("Moi", response.medicalHistory());
        // Khong duoc goi patientRepository (khong tao ban ghi moi) khi da co san profile.
        verify(patientRepository, never()).findById(any());
    }
}
