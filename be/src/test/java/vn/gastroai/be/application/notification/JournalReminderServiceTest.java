package vn.gastroai.be.application.notification;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import vn.gastroai.be.infrastructure.persistence.postgres.BristolLogRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.FoodDiaryEntryRepository;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JournalReminderServiceTest {

    private final FoodDiaryEntryRepository foodRepository = mock(FoodDiaryEntryRepository.class);
    private final BristolLogRepository bristolRepository = mock(BristolLogRepository.class);
    private final AppNotificationService notificationService = mock(AppNotificationService.class);
    private final JournalReminderService service = new JournalReminderService(
            foodRepository, bristolRepository, notificationService);

    @Test
    void doesNotRemindWhenBothJournalsExist() {
        mockEntries(true, true);

        service.remindIfJournalMissing(7L, LocalDate.of(2026, 10, 12));

        verify(notificationService, never()).createJournalReminder(anyLong(), any(), any(), any());
    }

    @Test
    void remindsForBothMissingWithOneDailyDedupeKey() {
        mockEntries(false, false);

        service.remindIfJournalMissing(7L, LocalDate.of(2026, 10, 12));

        verify(notificationService).createJournalReminder(
                eq(7L),
                eq("Bạn chưa ghi nhật ký ăn uống và Bristol hôm nay. Hãy bổ sung để theo dõi sức khỏe tiêu hóa đầy đủ hơn."),
                eq("/food-diary"),
                eq("JOURNAL_REMINDER:7:2026-10-12"));
    }

    @Test
    void remindsOnlyForFoodWhenBristolAlreadyExists() {
        mockEntries(false, true);

        service.remindIfJournalMissing(7L, LocalDate.of(2026, 10, 12));

        verify(notificationService).createJournalReminder(eq(7L),
                eq("Bạn chưa ghi nhật ký ăn uống hôm nay. Hãy dành một phút để cập nhật bữa ăn của mình."),
                eq("/food-diary"), eq("JOURNAL_REMINDER:7:2026-10-12"));
    }

    @Test
    void usesVietnamCalendarDayBoundaries() {
        mockEntries(true, true);
        ArgumentCaptor<Instant> from = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> to = ArgumentCaptor.forClass(Instant.class);

        service.remindIfJournalMissing(7L, LocalDate.of(2026, 10, 12));

        verify(foodRepository).existsByPatientIdAndEatenAtGreaterThanEqualAndEatenAtLessThan(
                eq(7L), from.capture(), to.capture());
        assertEquals(Instant.parse("2026-10-11T17:00:00Z"), from.getValue());
        assertEquals(Instant.parse("2026-10-12T17:00:00Z"), to.getValue());
    }

    private void mockEntries(boolean food, boolean bristol) {
        when(foodRepository.existsByPatientIdAndEatenAtGreaterThanEqualAndEatenAtLessThan(
                anyLong(), any(), any())).thenReturn(food);
        when(bristolRepository.existsByPatientIdAndLoggedAtGreaterThanEqualAndLoggedAtLessThan(
                anyLong(), any(), any())).thenReturn(bristol);
    }
}
