package vn.gastroai.be.application.patient;

import org.junit.jupiter.api.Test;
import vn.gastroai.be.api.patient.DailyCountPoint;
import vn.gastroai.be.api.patient.FoodDiaryEntryRequest;
import vn.gastroai.be.api.patient.FoodDiaryEntryResponse;
import vn.gastroai.be.api.patient.FoodDiaryTrashItem;
import vn.gastroai.be.api.patient.FoodDiaryTrendResponse;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.patient.FoodDiaryEntry;
import vn.gastroai.be.infrastructure.persistence.postgres.FoodDiaryEntryRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FoodDiaryServiceTest {

    private final FoodDiaryEntryRepository foodDiaryEntryRepository = mock(FoodDiaryEntryRepository.class);
    private final PatientRepository patientRepository = mock(PatientRepository.class);
    // UC0020 - them tham so retentionDays (30 ngay) vao cuoi constructor.
    private final FoodDiaryService service = new FoodDiaryService(
            foodDiaryEntryRepository, patientRepository, 30);

    @Test
    void updateThrowsWhenEntryBelongsToAnotherPatient() {
        Patient otherPatient = new Patient();
        otherPatient.setId(2L);
        FoodDiaryEntry entry = new FoodDiaryEntry(otherPatient, Instant.now(), "Com ga", null);
        when(foodDiaryEntryRepository.findById(99L)).thenReturn(Optional.of(entry));

        assertThrows(IllegalArgumentException.class,
                () -> service.update(1L, 99L, new FoodDiaryEntryRequest(Instant.now(), "Sua roi", null)));
    }

    @Test
    void createLinksEntryToRequestingPatient() {
        Patient patient = new Patient();
        patient.setId(1L);
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(foodDiaryEntryRepository.save(any(FoodDiaryEntry.class))).thenAnswer(inv -> {
            FoodDiaryEntry saved = inv.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        FoodDiaryEntryResponse response = service.create(1L,
                new FoodDiaryEntryRequest(Instant.parse("2026-01-01T12:00:00Z"), "Pho bo", "An sang"));

        assertEquals(10L, response.id());
        assertEquals("Pho bo", response.description());
    }

    @Test
    void trendFillsZeroForDaysWithNoEntries() {
        Instant now = Instant.now();
        Patient patient = new Patient();
        patient.setId(1L);
        // Chi 1 entry duy nhat, dung gio hien tai - cac ngay con lai trong khoang phai tra ve 0.
        FoodDiaryEntry entry = new FoodDiaryEntry(patient, now, "Com trua", null);
        when(foodDiaryEntryRepository.findByPatientIdAndEatenAtBetweenOrderByEatenAtAsc(anyLong(), any(), any()))
                .thenReturn(List.of(entry));

        FoodDiaryTrendResponse trend = service.trend(1L, 7);

        // days=7 phai ra DUNG 7 diem (ke ca hom nay) - truoc day co bug off-by-one ra 8.
        assertEquals(7, trend.points().size());
        long totalCount = trend.points().stream().mapToLong(DailyCountPoint::count).sum();
        assertEquals(1, totalCount); // dung 1 entry duy nhat trong toan bo khoang
    }

    // ===== UC0020 - thung rac (soft-delete) =====

    @Test
    void deleteSetsDeletedAtInsteadOfRemovingRow() {
        Patient patient = new Patient();
        patient.setId(1L);
        FoodDiaryEntry entry = new FoodDiaryEntry(patient, Instant.now(), "Pho bo", null);
        when(foodDiaryEntryRepository.findById(99L)).thenReturn(Optional.of(entry));
        when(foodDiaryEntryRepository.save(any(FoodDiaryEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        service.delete(1L, 99L);

        assertNotNull(entry.getDeletedAt());
        // Phai la soft-delete: KHONG duoc goi repository.delete(...) that su.
        verify(foodDiaryEntryRepository, never()).delete(any());
    }

    @Test
    void restoreThrowsNotFoundWhenNoDeletedRowMatches() {
        when(foodDiaryEntryRepository.findDeletedOwned(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.restore(1L, 99L));
    }

    @Test
    void restoreThrowsConflictWhenPastRetentionWindow() {
        Patient patient = new Patient();
        patient.setId(1L);
        FoodDiaryEntry entry = new FoodDiaryEntry(patient, Instant.now(), "Pho bo", null);
        entry.setDeletedAt(Instant.now().minus(31, ChronoUnit.DAYS));
        when(foodDiaryEntryRepository.findDeletedOwned(99L, 1L)).thenReturn(Optional.of(entry));

        assertThrows(IllegalStateException.class, () -> service.restore(1L, 99L));
    }

    @Test
    void restoreClearsDeletedAtWhenWithinRetentionWindow() {
        Patient patient = new Patient();
        patient.setId(1L);
        FoodDiaryEntry entry = new FoodDiaryEntry(patient, Instant.now(), "Pho bo", null);
        entry.setDeletedAt(Instant.now().minus(1, ChronoUnit.DAYS));
        when(foodDiaryEntryRepository.findDeletedOwned(99L, 1L)).thenReturn(Optional.of(entry));
        when(foodDiaryEntryRepository.save(any(FoodDiaryEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        FoodDiaryEntryResponse response = service.restore(1L, 99L);

        assertNull(entry.getDeletedAt());
        assertEquals("Pho bo", response.description());
    }

    @Test
    void listTrashComputesPurgeAtFromDeletedAt() {
        Patient patient = new Patient();
        patient.setId(1L);
        FoodDiaryEntry entry = new FoodDiaryEntry(patient, Instant.now(), "Pho bo", null);
        Instant deletedAt = Instant.parse("2026-01-01T00:00:00Z");
        entry.setDeletedAt(deletedAt);
        when(foodDiaryEntryRepository.findTrash(anyLong(), any())).thenReturn(List.of(entry));

        List<FoodDiaryTrashItem> trash = service.listTrash(1L);

        assertEquals(1, trash.size());
        assertEquals(deletedAt, trash.get(0).deletedAt());
        assertEquals(deletedAt.plus(30, ChronoUnit.DAYS), trash.get(0).purgeAt());
    }
}