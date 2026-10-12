package vn.gastroai.be.application.patient;

import org.junit.jupiter.api.Test;
import vn.gastroai.be.infrastructure.persistence.postgres.BristolLogRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.FoodDiaryEntryRepository;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JournalTrashCleanupServiceTest {

    private final FoodDiaryEntryRepository foodDiaryEntryRepository = mock(FoodDiaryEntryRepository.class);
    private final BristolLogRepository bristolLogRepository = mock(BristolLogRepository.class);
    private final JournalTrashCleanupService service = new JournalTrashCleanupService(
            foodDiaryEntryRepository, bristolLogRepository, 30);

    @Test
    void purgeExpiredPassesCutoffApproximatelyThirtyDaysAgo() {
        when(foodDiaryEntryRepository.purgeDeletedBefore(org.mockito.ArgumentMatchers.any())).thenReturn(0);
        when(bristolLogRepository.purgeDeletedBefore(org.mockito.ArgumentMatchers.any())).thenReturn(0);

        service.purgeExpired();

        var foodDiaryCutoffCaptor = forClass(Instant.class);
        var bristolCutoffCaptor = forClass(Instant.class);
        verify(foodDiaryEntryRepository).purgeDeletedBefore(foodDiaryCutoffCaptor.capture());
        verify(bristolLogRepository).purgeDeletedBefore(bristolCutoffCaptor.capture());

        // Dung sai +-1 phut vi Instant.now() trong test va trong service khong the trung
        // tuyet doi, chi can "xap xi bay gio tru 30 ngay" la du, giong cach HealthReportServiceTest
        // kiem tra ArgumentCaptor<Instant>.
        Instant expectedCutoff = Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS);
        assertTrue(Duration.between(expectedCutoff, foodDiaryCutoffCaptor.getValue()).abs().toMinutes() <= 1);
        assertTrue(Duration.between(expectedCutoff, bristolCutoffCaptor.getValue()).abs().toMinutes() <= 1);
    }
}