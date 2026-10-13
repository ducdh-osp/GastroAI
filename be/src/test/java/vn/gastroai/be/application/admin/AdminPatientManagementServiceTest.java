package vn.gastroai.be.application.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminPatientManagementServiceTest {
    private final PatientRepository patients = mock(PatientRepository.class);
    private AdminPatientManagementService service;

    @BeforeEach
    void setUp() {
        service = new AdminPatientManagementService(patients);
    }

    @Test
    void lockMarksAccountAndInvalidatesAllExistingTokensExactlyOnce() {
        Patient patient = patient();
        patient.setTokenVersion(4);
        when(patients.findByIdForUpdate(1L)).thenReturn(Optional.of(patient));

        var result = service.lock(1L);

        assertTrue(result.adminLocked());
        assertNotNull(result.adminLockedAt());
        assertEquals(5, patient.getTokenVersion());

        service.lock(1L);
        assertEquals(5, patient.getTokenVersion());
    }

    @Test
    void unlockDoesNotRestoreInvalidatedTokens() {
        Patient patient = patient();
        patient.setAdminLocked(true);
        patient.setAdminLockedAt(Instant.now());
        patient.setTokenVersion(5);
        when(patients.findByIdForUpdate(1L)).thenReturn(Optional.of(patient));

        var result = service.unlock(1L);

        assertFalse(result.adminLocked());
        assertNull(result.adminLockedAt());
        assertEquals(5, patient.getTokenVersion());
    }

    private Patient patient() {
        Patient patient = new Patient();
        patient.setId(1L);
        patient.setEmail("patient@example.com");
        return patient;
    }
}
