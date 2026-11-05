package vn.gastroai.be.application.auth;

import org.junit.jupiter.api.Test;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UnverifiedPatientCleanupServiceTest {
    private final PatientRepository patients = mock(PatientRepository.class);
    private final UnverifiedPatientCleanupService service = new UnverifiedPatientCleanupService(patients);

    @Test
    void deletesOnlyExpiredUnverifiedAccounts() {
        when(patients.deleteExpiredUnverified(any(Instant.class))).thenReturn(2L);

        assertEquals(2L, service.cleanupExpiredAccounts());
        verify(patients).deleteExpiredUnverified(any(Instant.class));
    }
}
