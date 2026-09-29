package vn.gastroai.be.application.patient;

import org.junit.jupiter.api.Test;
import vn.gastroai.be.api.patient.DailyCountPoint;
import vn.gastroai.be.api.patient.FoodDiaryEntryRequest;
import vn.gastroai.be.api.patient.FoodDiaryEntryResponse;
import vn.gastroai.be.api.patient.FoodDiaryTrendResponse;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.patient.FoodDiaryEntry;
import vn.gastroai.be.infrastructure.persistence.postgres.FoodDiaryEntryRepository;
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

class FoodDiaryServiceTest {

    private final FoodDiaryEntryRepository foodDiaryEntryRepository = mock(FoodDiaryEntryRepository.class);
    private final PatientRepository patientRepository = mock(PatientRepository.class);
    private final FoodDiaryService service = new FoodDiaryService(foodDiaryEntryRepository, patientRepository);

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
}
