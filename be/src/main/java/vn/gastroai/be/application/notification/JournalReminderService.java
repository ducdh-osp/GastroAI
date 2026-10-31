package vn.gastroai.be.application.notification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.infrastructure.persistence.postgres.BristolLogRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.FoodDiaryEntryRepository;

import java.time.LocalDate;

import static vn.gastroai.be.application.support.VietnamDateRange.ZONE;

@Service
public class JournalReminderService {

    private final FoodDiaryEntryRepository foodDiaryRepository;
    private final BristolLogRepository bristolRepository;
    private final AppNotificationService notificationService;
    private final PatientNotificationSettingService settingService;

    public JournalReminderService(FoodDiaryEntryRepository foodDiaryRepository,
                                  BristolLogRepository bristolRepository,
                                  AppNotificationService notificationService) {
        this(foodDiaryRepository, bristolRepository, notificationService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public JournalReminderService(FoodDiaryEntryRepository foodDiaryRepository,
                                  BristolLogRepository bristolRepository,
                                  AppNotificationService notificationService,
                                  PatientNotificationSettingService settingService) {
        this.foodDiaryRepository = foodDiaryRepository;
        this.bristolRepository = bristolRepository;
        this.notificationService = notificationService;
        this.settingService = settingService;
    }

    @Transactional("postgresTransactionManager")
    public void remindIfJournalMissing(Long patientId, LocalDate date) {
        if (settingService != null && !settingService.isJournalReminderEnabled(patientId)) {
            return;
        }
        var start = date.atStartOfDay(ZONE).toInstant();
        var end = date.plusDays(1).atStartOfDay(ZONE).toInstant();
        boolean hasFood = foodDiaryRepository.existsByPatientIdAndEatenAtGreaterThanEqualAndEatenAtLessThan(
                patientId, start, end);
        boolean hasBristol = bristolRepository.existsByPatientIdAndLoggedAtGreaterThanEqualAndLoggedAtLessThan(
                patientId, start, end);
        if (hasFood && hasBristol) {
            return;
        }

        String message;
        String actionUrl;
        if (!hasFood && !hasBristol) {
            message = "Bạn chưa ghi nhật ký ăn uống và Bristol hôm nay. Hãy bổ sung để theo dõi sức khỏe tiêu hóa đầy đủ hơn.";
            actionUrl = "/food-diary";
        } else if (!hasFood) {
            message = "Bạn chưa ghi nhật ký ăn uống hôm nay. Hãy dành một phút để cập nhật bữa ăn của mình.";
            actionUrl = "/food-diary";
        } else {
            message = "Bạn chưa ghi nhật ký Bristol hôm nay. Hãy cập nhật để theo dõi diễn tiến tiêu hóa.";
            actionUrl = "/bristol";
        }
        notificationService.createJournalReminder(patientId, message, actionUrl,
                "JOURNAL_REMINDER:" + patientId + ":" + date);
    }
}
