package vn.gastroai.be.application.notification;

import org.junit.jupiter.api.Test;
import vn.gastroai.be.api.notification.NotificationResponse;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.notification.MedicationConfirmation;
import vn.gastroai.be.domain.notification.MedicationReminder;
import vn.gastroai.be.infrastructure.persistence.postgres.MedicationConfirmationRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.MedicationReminderRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MedicationDueNotificationServiceTest {

    private final MedicationReminderRepository reminderRepository = mock(MedicationReminderRepository.class);
    private final MedicationConfirmationRepository confirmationRepository = mock(MedicationConfirmationRepository.class);
    private final AppNotificationService notificationService = mock(AppNotificationService.class);
    private final PatientNotificationSettingService settingService = mock(PatientNotificationSettingService.class);
    private final EmailOutboxService emailOutboxService = mock(EmailOutboxService.class);
    private final MedicationDueNotificationService service = new MedicationDueNotificationService(
            reminderRepository, confirmationRepository, notificationService, settingService, emailOutboxService);

    @Test
    void createsNotificationOnlyForDoseThatIsAlreadyDue() {
        LocalDate date = LocalDate.of(2026, 10, 23);
        MedicationReminder reminder = reminder(5L, List.of(LocalTime.of(8, 0), LocalTime.of(20, 0)));
        when(reminderRepository.findActiveForDate(date)).thenReturn(List.of(reminder));
        when(confirmationRepository.findByReminderIdInAndConfirmationDate(List.of(5L), date))
                .thenReturn(List.of());
        when(settingService.isMedicationReminderEnabled(7L)).thenReturn(true);
        when(notificationService.createMedicationReminder(any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(mock(NotificationResponse.class)));

        int created = service.notifyDueDoses(date, LocalTime.of(12, 0));

        assertEquals(1, created);
        verify(notificationService).createMedicationReminder(
                eq(7L), eq(5L), eq("Đến giờ uống thuốc"),
                eq("Bạn chưa xác nhận liều Omeprazole (20 mg) lúc 08:00 hôm nay. "
                        + "Hãy dùng thuốc theo đúng hướng dẫn và xác nhận sau khi uống."),
                eq("MEDICATION_REMINDER:5:2026-10-23:08:00"),
                eq("08:00"));
        verify(emailOutboxService).enqueueMedicationReminder(reminder, date, LocalTime.of(8, 0));
    }

    @Test
    void skipsDoseAlreadyConfirmedToday() {
        LocalDate date = LocalDate.of(2026, 10, 23);
        MedicationReminder reminder = reminder(5L, List.of(LocalTime.of(8, 0)));
        MedicationConfirmation confirmation = new MedicationConfirmation(
                reminder, Instant.parse("2026-10-23T01:05:00Z"), date, LocalTime.of(8, 0));
        when(reminderRepository.findActiveForDate(date)).thenReturn(List.of(reminder));
        when(confirmationRepository.findByReminderIdInAndConfirmationDate(List.of(5L), date))
                .thenReturn(List.of(confirmation));
        when(settingService.isMedicationReminderEnabled(7L)).thenReturn(true);

        assertEquals(0, service.notifyDueDoses(date, LocalTime.of(12, 0)));

        verify(notificationService, never()).createMedicationReminder(any(), any(), any(), any(), any(), any());
        verify(emailOutboxService, never()).enqueueMedicationReminder(any(), any(), any());
    }

    @Test
    void skipsInAppAndEmailReminderWhenPatientDisabledMedicationNotifications() {
        LocalDate date = LocalDate.of(2026, 10, 23);
        MedicationReminder reminder = reminder(5L, List.of(LocalTime.of(8, 0)));
        when(reminderRepository.findActiveForDate(date)).thenReturn(List.of(reminder));
        when(confirmationRepository.findByReminderIdInAndConfirmationDate(List.of(5L), date))
                .thenReturn(List.of());
        when(settingService.isMedicationReminderEnabled(7L)).thenReturn(false);

        assertEquals(0, service.notifyDueDoses(date, LocalTime.NOON));

        verify(notificationService, never()).createMedicationReminder(any(), any(), any(), any(), any(), any());
        verify(emailOutboxService, never()).enqueueMedicationReminder(any(), any(), any());
    }

    @Test
    void avoidsConfirmationQueryWhenThereAreNoActiveRegimens() {
        LocalDate date = LocalDate.of(2026, 10, 23);
        when(reminderRepository.findActiveForDate(date)).thenReturn(List.of());

        assertEquals(0, service.notifyDueDoses(date, LocalTime.NOON));

        verify(confirmationRepository, never()).findByReminderIdInAndConfirmationDate(anyList(), any());
        verify(emailOutboxService, never()).enqueueMedicationReminder(any(), any(), any());
    }

    @Test
    void notifyDueDosesForPatientScopesToSinglePatientAndCreatesNotification() {
        LocalDate date = LocalDate.of(2026, 10, 23);
        MedicationReminder reminder = reminder(5L, List.of(LocalTime.of(8, 0)));
        when(reminderRepository.findActiveForDateAndPatientId(7L, date)).thenReturn(List.of(reminder));
        when(confirmationRepository.findByReminderIdInAndConfirmationDate(List.of(5L), date))
                .thenReturn(List.of());
        when(settingService.isMedicationReminderEnabled(7L)).thenReturn(true);
        when(notificationService.createMedicationReminder(any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(mock(NotificationResponse.class)));

        int created = service.notifyDueDosesForPatient(7L, date, LocalTime.of(12, 0));

        assertEquals(1, created);
        // Quan trong: phai goi findActiveForDateAndPatientId scoped theo patientId, KHONG goi findActiveForDate
        verify(reminderRepository).findActiveForDateAndPatientId(7L, date);
        verify(reminderRepository, never()).findActiveForDate(any());
        verify(notificationService).createMedicationReminder(
                eq(7L), eq(5L), eq("Đến giờ uống thuốc"),
                eq("Bạn chưa xác nhận liều Omeprazole (20 mg) lúc 08:00 hôm nay. "
                        + "Hãy dùng thuốc theo đúng hướng dẫn và xác nhận sau khi uống."),
                eq("MEDICATION_REMINDER:5:2026-10-23:08:00"),
                eq("08:00"));
        verify(emailOutboxService).enqueueMedicationReminder(reminder, date, LocalTime.of(8, 0));
    }

    @Test
    void notifyDueDosesForPatientSkipsDosesWhenPatientDisabledNotifications() {
        LocalDate date = LocalDate.of(2026, 10, 23);
        MedicationReminder reminder = reminder(5L, List.of(LocalTime.of(8, 0)));
        when(reminderRepository.findActiveForDateAndPatientId(7L, date)).thenReturn(List.of(reminder));
        when(confirmationRepository.findByReminderIdInAndConfirmationDate(List.of(5L), date))
                .thenReturn(List.of());
        when(settingService.isMedicationReminderEnabled(7L)).thenReturn(false);

        int created = service.notifyDueDosesForPatient(7L, date, LocalTime.of(12, 0));

        assertEquals(0, created);
        verify(reminderRepository).findActiveForDateAndPatientId(7L, date);
        verify(notificationService, never()).createMedicationReminder(any(), any(), any(), any(), any(), any());
        verify(emailOutboxService, never()).enqueueMedicationReminder(any(), any(), any());
    }

    @Test
    void notifyDueDosesForPatientSkipsAlreadyConfirmedDose() {
        LocalDate date = LocalDate.of(2026, 10, 23);
        MedicationReminder reminder = reminder(5L, List.of(LocalTime.of(8, 0)));
        MedicationConfirmation confirmation = new MedicationConfirmation(
                reminder, Instant.parse("2026-10-23T01:05:00Z"), date, LocalTime.of(8, 0));
        when(reminderRepository.findActiveForDateAndPatientId(7L, date)).thenReturn(List.of(reminder));
        when(confirmationRepository.findByReminderIdInAndConfirmationDate(List.of(5L), date))
                .thenReturn(List.of(confirmation));
        when(settingService.isMedicationReminderEnabled(7L)).thenReturn(true);

        int created = service.notifyDueDosesForPatient(7L, date, LocalTime.of(12, 0));

        assertEquals(0, created);
        verify(reminderRepository).findActiveForDateAndPatientId(7L, date);
        verify(notificationService, never()).createMedicationReminder(any(), any(), any(), any(), any(), any());
        verify(emailOutboxService, never()).enqueueMedicationReminder(any(), any(), any());
    }

    private MedicationReminder reminder(Long id, List<LocalTime> times) {
        Patient patient = new Patient();
        patient.setId(7L);
        MedicationReminder reminder = new MedicationReminder(
                patient, "Omeprazole", "20 mg", times, null, null, null, true);
        reminder.setId(id);
        return reminder;
    }
}
