package vn.gastroai.be.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.gastroai.be.application.notification.MedicationDueNotificationService;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class MedicationReminderNotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(MedicationReminderNotificationScheduler.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final MedicationDueNotificationService notificationService;

    public MedicationReminderNotificationScheduler(MedicationDueNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** Kiem tra moi phut; dedupe key dam bao moi lieu/ngay chi tao mot thong bao. */
    @Scheduled(cron = "${app.notification.medication-reminder-cron:0 * * * * *}",
            zone = "Asia/Ho_Chi_Minh")
    public void remindDueDoses() {
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        try {
            notificationService.notifyDueDoses(
                    now.toLocalDate(), now.toLocalTime().withSecond(0).withNano(0));
        } catch (RuntimeException exception) {
            log.error("Khong the tao thong bao nhac uong thuoc: {}", exception.getMessage(), exception);
        }
    }
}
