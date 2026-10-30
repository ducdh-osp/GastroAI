package vn.gastroai.be.application.notification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.domain.notification.MedicationConfirmation;
import vn.gastroai.be.domain.notification.MedicationReminder;
import vn.gastroai.be.infrastructure.persistence.postgres.MedicationConfirmationRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.MedicationReminderRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Tao thong bao cho tung lieu thuoc da den gio trong ngay nhung chua duoc xac nhan. */
@Service
public class MedicationDueNotificationService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final MedicationReminderRepository reminderRepository;
    private final MedicationConfirmationRepository confirmationRepository;
    private final AppNotificationService notificationService;
    private final PatientNotificationSettingService settingService;
    private final EmailOutboxService emailOutboxService;

    public MedicationDueNotificationService(MedicationReminderRepository reminderRepository,
                                            MedicationConfirmationRepository confirmationRepository,
                                            AppNotificationService notificationService,
                                            PatientNotificationSettingService settingService,
                                            EmailOutboxService emailOutboxService) {
        this.reminderRepository = reminderRepository;
        this.confirmationRepository = confirmationRepository;
        this.notificationService = notificationService;
        this.settingService = settingService;
        this.emailOutboxService = emailOutboxService;
    }

    @Transactional("postgresTransactionManager")
    public int notifyDueDoses(LocalDate date, LocalTime currentTime) {
        List<MedicationReminder> reminders = reminderRepository.findActiveForDate(date);
        return processReminders(reminders, date, currentTime);
    }

    @Transactional("postgresTransactionManager")
    public int notifyDueDosesForPatient(Long patientId, LocalDate date, LocalTime currentTime) {
        List<MedicationReminder> reminders = reminderRepository.findActiveForDateAndPatientId(patientId, date);
        return processReminders(reminders, date, currentTime);
    }

    private int processReminders(List<MedicationReminder> reminders, LocalDate date, LocalTime currentTime) {
        if (reminders.isEmpty()) {
            return 0;
        }

        List<Long> reminderIds = reminders.stream().map(MedicationReminder::getId).toList();
        Set<DoseKey> confirmedDoses = new HashSet<>();
        for (MedicationConfirmation confirmation
                : confirmationRepository.findByReminderIdInAndConfirmationDate(reminderIds, date)) {
            confirmedDoses.add(new DoseKey(
                    confirmation.getReminder().getId(), confirmation.getScheduledTime()));
        }

        int created = 0;
        for (MedicationReminder reminder : reminders) {
            if (!settingService.isMedicationReminderEnabled(reminder.getPatient().getId())) {
                continue;
            }
            for (LocalTime scheduledTime : reminder.getTimesOfDay()) {
                if (scheduledTime.isAfter(currentTime)
                        || confirmedDoses.contains(new DoseKey(reminder.getId(), scheduledTime))) {
                    continue;
                }

                String time = scheduledTime.format(TIME_FORMAT);
                String dosage = reminder.getDosage() == null || reminder.getDosage().isBlank()
                        ? ""
                        : " (" + reminder.getDosage().trim() + ")";
                String message = "Bạn chưa xác nhận liều " + reminder.getMedicineName() + dosage
                        + " lúc " + time + " hôm nay. Hãy dùng thuốc theo đúng hướng dẫn và xác nhận sau khi uống.";
                String dedupeKey = "MEDICATION_REMINDER:" + reminder.getId() + ":" + date + ":" + time;

                if (notificationService.createMedicationReminder(
                        reminder.getPatient().getId(), reminder.getId(),
                        "Đến giờ uống thuốc", message, dedupeKey).isPresent()) {
                    created++;
                }
                // Outbox co dedupe rieng. Worker doi grace period, sau do kiem tra lai online
                // va trang thai xac nhan truoc khi gui email chung chung (khong lo ten/lieu thuoc).
                emailOutboxService.enqueueMedicationReminder(reminder, date, scheduledTime);
            }
        }
        return created;
    }

    private record DoseKey(Long reminderId, LocalTime scheduledTime) {
    }
}
