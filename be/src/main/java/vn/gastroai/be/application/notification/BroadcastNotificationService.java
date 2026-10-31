package vn.gastroai.be.application.notification;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.api.notification.BroadcastAnnouncementRequest;
import vn.gastroai.be.api.notification.BroadcastAnnouncementResponse;
import vn.gastroai.be.api.notification.NotificationResponse;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.domain.notification.AppNotification;
import vn.gastroai.be.domain.notification.BroadcastAnnouncement;
import vn.gastroai.be.domain.notification.NotificationCreatedEvent;
import vn.gastroai.be.domain.notification.NotificationRecipientType;
import vn.gastroai.be.domain.notification.NotificationType;
import vn.gastroai.be.infrastructure.persistence.postgres.AppNotificationRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.BroadcastAnnouncementRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;

import java.util.List;

/**
 * UC0075 - Admin gui thong bao broadcast toi toan bo hoac mot nhom benh nhan
 * (vd: bao tri he thong, tai lieu y khoa moi...).
 */
@Service
public class BroadcastNotificationService {

    private final BroadcastAnnouncementRepository broadcastRepository;
    private final AppNotificationRepository notificationRepository;
    private final PatientRepository patientRepository;
    private final PatientNotificationSettingService settingService;
    private final ApplicationEventPublisher eventPublisher;

    public BroadcastNotificationService(BroadcastAnnouncementRepository broadcastRepository,
                                       AppNotificationRepository notificationRepository,
                                       PatientRepository patientRepository,
                                       PatientNotificationSettingService settingService,
                                       ApplicationEventPublisher eventPublisher) {
        this.broadcastRepository = broadcastRepository;
        this.notificationRepository = notificationRepository;
        this.patientRepository = patientRepository;
        this.settingService = settingService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional("postgresTransactionManager")
    public BroadcastAnnouncementResponse sendBroadcast(Long adminId, BroadcastAnnouncementRequest request) {
        String targetGroup = request.targetGroup() == null || request.targetGroup().isBlank()
                ? "ALL"
                : request.targetGroup().trim().toUpperCase();

        List<Long> candidateIds = patientRepository.findAllVerifiedIds();

        // Loc nhung benh nhan cho phep nhan thong bao he thong/broadcast (batch query 1 lan tranh N+1)
        List<Long> targetIds = settingService.filterSystemBroadcastEnabledPatientIds(candidateIds);

        BroadcastAnnouncement announcement = new BroadcastAnnouncement(
                request.title().trim(),
                request.message().trim(),
                targetGroup,
                request.actionUrl(),
                adminId
        );
        announcement = broadcastRepository.save(announcement);

        boolean sendRealtime = request.sendWebSocket() == null || request.sendWebSocket();
        BroadcastAnnouncement finalAnnouncement = announcement;

        // Batch insert toan bo thong bao thay vi vong lap save tung record
        List<AppNotification> notificationsToSave = targetIds.stream()
                .map(patientId -> new AppNotification(
                        NotificationRecipientType.PATIENT,
                        patientId,
                        NotificationType.SYSTEM,
                        finalAnnouncement.getTitle(),
                        finalAnnouncement.getMessage(),
                        "BROADCAST",
                        finalAnnouncement.getId(),
                        finalAnnouncement.getActionUrl(),
                        "BROADCAST:" + finalAnnouncement.getId() + ":" + patientId
                ))
                .toList();

        List<AppNotification> savedNotifications = notificationRepository.saveAll(notificationsToSave);
        int sentCount = savedNotifications.size();

        if (sendRealtime) {
            for (AppNotification notification : savedNotifications) {
                NotificationResponse notifResponse = new NotificationResponse(
                        notification.getId(),
                        notification.getNotificationType().name(),
                        notification.getTitle(),
                        notification.getMessage(),
                        notification.getSourceType(),
                        notification.getSourceId(),
                        notification.getActionUrl(),
                        notification.getCreatedAt(),
                        null
                );
                eventPublisher.publishEvent(new NotificationCreatedEvent(
                        NotificationRecipientType.PATIENT,
                        notification.getRecipientId(),
                        notifResponse
                ));
            }
        }

        announcement.setRecipientCount(sentCount);
        announcement = broadcastRepository.save(announcement);

        return toResponse(announcement);
    }

    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public Page<BroadcastAnnouncementResponse> listBroadcasts(Pageable pageable) {
        return broadcastRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toResponse);
    }

    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public BroadcastAnnouncementResponse getBroadcast(Long id) {
        return broadcastRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay thong bao broadcast"));
    }

    private BroadcastAnnouncementResponse toResponse(BroadcastAnnouncement announcement) {
        return new BroadcastAnnouncementResponse(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getMessage(),
                announcement.getTargetGroup(),
                announcement.getActionUrl(),
                announcement.getCreatedByAdminId(),
                announcement.getRecipientCount(),
                announcement.getCreatedAt()
        );
    }
}
