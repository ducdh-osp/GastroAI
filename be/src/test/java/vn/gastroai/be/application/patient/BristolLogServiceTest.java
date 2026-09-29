package vn.gastroai.be.application.patient;

import org.junit.jupiter.api.Test;
import vn.gastroai.be.api.patient.BristolLogRequest;
import vn.gastroai.be.api.patient.BristolLogResponse;
import vn.gastroai.be.api.patient.BristolTrendResponse;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.patient.BristolLog;
import vn.gastroai.be.infrastructure.persistence.postgres.BristolLogRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BristolLogServiceTest {

    private final BristolLogRepository bristolLogRepository = mock(BristolLogRepository.class);
    private final PatientRepository patientRepository = mock(PatientRepository.class);
    private final BristolLogService service = new BristolLogService(bristolLogRepository, patientRepository);

    @Test
    void updateThrowsWhenLogBelongsToAnotherPatient() {
        Patient otherPatient = new Patient();
        otherPatient.setId(2L);
        BristolLog log = new BristolLog(otherPatient, Instant.now(), 4, null);
        when(bristolLogRepository.findById(99L)).thenReturn(Optional.of(log));

        assertThrows(IllegalArgumentException.class,
                () -> service.update(1L, 99L, new BristolLogRequest(Instant.now(), 5, null)));
    }

    @Test
    void createLinksLogToRequestingPatient() {
        Patient patient = new Patient();
        patient.setId(1L);
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(bristolLogRepository.save(any(BristolLog.class))).thenAnswer(inv -> {
            BristolLog saved = inv.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        BristolLogResponse response = service.create(1L, new BristolLogRequest(Instant.now(), 6, "Hoi long"));

        assertEquals(10L, response.id());
        assertEquals(6, response.bristolType());
    }

    @Test
    void trendReturnsOnePointPerLogWithoutGrouping() {
        Patient patient = new Patient();
        patient.setId(1L);
        BristolLog first = new BristolLog(patient, Instant.parse("2026-01-01T08:00:00Z"), 3, null);
        BristolLog second = new BristolLog(patient, Instant.parse("2026-01-01T20:00:00Z"), 5, null);
        when(bristolLogRepository.findByPatientIdAndLoggedAtBetweenOrderByLoggedAtAsc(anyLong(), any(), any()))
                .thenReturn(List.of(first, second));

        BristolTrendResponse trend = service.trend(1L, 7);

        // 2 lan ghi nhan cung 1 ngay phai la 2 diem rieng, khong gop lam 1 nhu food-diary.
        assertEquals(2, trend.points().size());
        assertEquals(3, trend.points().get(0).bristolType());
        assertEquals(5, trend.points().get(1).bristolType());
    }
}
