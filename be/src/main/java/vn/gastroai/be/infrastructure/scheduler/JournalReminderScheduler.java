package vn.gastroai.be.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.gastroai.be.application.notification.JournalReminderService;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class JournalReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(JournalReminderScheduler.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final PatientRepository patientRepository;
    private final JournalReminderService reminderService;

    public JournalReminderScheduler(PatientRepository patientRepository,
                                    JournalReminderService reminderService) {
        this.patientRepository = patientRepository;
        this.reminderService = reminderService;
    }

    /** Nhắc vào 20:00 giờ Việt Nam; có thể đổi qua app.notification.journal-reminder-cron. */
    @Scheduled(cron = "${app.notification.journal-reminder-cron:0 0 20 * * *}", zone = "Asia/Ho_Chi_Minh")
    public void remindPatients() {
        LocalDate today = LocalDate.now(ZONE);
        for (Long patientId : patientRepository.findAllVerifiedIds()) {
            try {
                reminderService.remindIfJournalMissing(patientId, today);
            } catch (RuntimeException exception) {
                log.error("Khong the tao nhac nhat ky cho patientId={}: {}",
                        patientId, exception.getMessage(), exception);
            }
        }
    }
}
