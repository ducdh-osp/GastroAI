package vn.gastroai.be.application.patient;

import org.junit.jupiter.api.Test;
import vn.gastroai.be.api.patient.BristolLogRequest;
import vn.gastroai.be.api.patient.BristolLogResponse;
import vn.gastroai.be.api.patient.BristolTrashItem;
import vn.gastroai.be.api.patient.BristolTrendResponse;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.patient.BristolLog;
import vn.gastroai.be.infrastructure.persistence.postgres.BristolLogRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class BristolLogServiceTest {

    private final BristolLogRepository bristolLogRepository = mock(BristolLogRepository.class);
    private final PatientRepository patientRepository = mock(PatientRepository.class);
    // UC0020 - them tham so retentionDays (30 ngay) vao cuoi constructor.
    private final BristolLogService service = new BristolLogService(bristolLogRepository, patientRepository, 30);

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

    @Test
    void trendUsesWholeVietnamCalendarDays() {
        when(bristolLogRepository.findByPatientIdAndLoggedAtBetweenOrderByLoggedAtAsc(anyLong(), any(), any()))
                .thenReturn(List.of());

        service.trend(1L, 1);

        var fromCaptor = forClass(Instant.class);
        var toCaptor = forClass(Instant.class);
        verify(bristolLogRepository).findByPatientIdAndLoggedAtBetweenOrderByLoggedAtAsc(
                org.mockito.ArgumentMatchers.eq(1L), fromCaptor.capture(), toCaptor.capture());
        ZoneId vietnam = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate today = Instant.now().atZone(vietnam).toLocalDate();
        assertEquals(today.atStartOfDay(vietnam).toInstant(), fromCaptor.getValue());
        assertEquals(today.plusDays(1).atStartOfDay(vietnam).toInstant(), toCaptor.getValue());
    }

    // ===== UC0020 - thung rac (soft-delete) =====

    @Test
    void deleteSetsDeletedAtInsteadOfRemovingRow() {
        Patient patient = new Patient();
        patient.setId(1L);
        BristolLog log = new BristolLog(patient, Instant.now(), 4, null);
        when(bristolLogRepository.findById(99L)).thenReturn(Optional.of(log));
        when(bristolLogRepository.save(any(BristolLog.class))).thenAnswer(inv -> inv.getArgument(0));

        service.delete(1L, 99L);

        assertNotNull(log.getDeletedAt());
        // Phai la soft-delete: KHONG duoc goi repository.delete(...) that su.
        verify(bristolLogRepository, never()).delete(any());
    }

    @Test
    void restoreThrowsNotFoundWhenNoDeletedRowMatches() {
        when(bristolLogRepository.findDeletedOwned(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.restore(1L, 99L));
    }

    @Test
    void restoreThrowsConflictWhenPastRetentionWindow() {
        Patient patient = new Patient();
        patient.setId(1L);
        BristolLog log = new BristolLog(patient, Instant.now(), 4, null);
        log.setDeletedAt(Instant.now().minus(31, ChronoUnit.DAYS));
        when(bristolLogRepository.findDeletedOwned(99L, 1L)).thenReturn(Optional.of(log));

        assertThrows(IllegalStateException.class, () -> service.restore(1L, 99L));
    }

    @Test
    void restoreClearsDeletedAtWhenWithinRetentionWindow() {
        Patient patient = new Patient();
        patient.setId(1L);
        BristolLog log = new BristolLog(patient, Instant.now(), 4, null);
        log.setDeletedAt(Instant.now().minus(1, ChronoUnit.DAYS));
        when(bristolLogRepository.findDeletedOwned(99L, 1L)).thenReturn(Optional.of(log));
        when(bristolLogRepository.save(any(BristolLog.class))).thenAnswer(inv -> inv.getArgument(0));

        BristolLogResponse response = service.restore(1L, 99L);

        assertNull(log.getDeletedAt());
        assertEquals(4, response.bristolType());
    }

    @Test
    void listTrashComputesPurgeAtFromDeletedAt() {
        Patient patient = new Patient();
        patient.setId(1L);
        BristolLog log = new BristolLog(patient, Instant.now(), 4, null);
        Instant deletedAt = Instant.parse("2026-01-01T00:00:00Z");
        log.setDeletedAt(deletedAt);
        when(bristolLogRepository.findTrash(anyLong(), any())).thenReturn(List.of(log));

        List<BristolTrashItem> trash = service.listTrash(1L);

        assertEquals(1, trash.size());
        assertEquals(deletedAt, trash.get(0).deletedAt());
        assertEquals(deletedAt.plus(30, ChronoUnit.DAYS), trash.get(0).purgeAt());
    }
}