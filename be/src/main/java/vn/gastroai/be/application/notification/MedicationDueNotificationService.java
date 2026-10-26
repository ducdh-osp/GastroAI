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

    public MedicationDueNotificationService(MedicationReminderRepository reminderRepository,
                                            MedicationConfirmationRepository confirmationRepository,
                                            AppNotificationService notificationService) {
        this.reminderRepository = reminderRepository;
        this.confirmationRepository = confirmationRepository;
        this.notificationService = notificationService;
    }

    @Transactional("postgresTransactionManager")
    public int notifyDueDoses(LocalDate date, LocalTime currentTime) {
        List<MedicationReminder> reminders = reminderRepository.findActiveForDate(date);
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
            }
        }
        return created;
    }

    private record DoseKey(Long reminderId, LocalTime scheduledTime) {
    }
}
