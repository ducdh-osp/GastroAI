package vn.gastroai.be.application.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.api.notification.NotificationPageResponse;
import vn.gastroai.be.api.notification.NotificationResponse;
import vn.gastroai.be.domain.notification.AppNotification;
import vn.gastroai.be.domain.notification.NotificationCreatedEvent;
import vn.gastroai.be.domain.notification.NotificationRecipientType;
import vn.gastroai.be.domain.notification.NotificationState;
import vn.gastroai.be.domain.notification.NotificationType;
import vn.gastroai.be.domain.triage.TriageAlertEvent;
import vn.gastroai.be.infrastructure.persistence.postgres.AppNotificationRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.NotificationStateRepository;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AppNotificationService {

    private final AppNotificationRepository notificationRepository;
    private final NotificationStateRepository stateRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public AppNotificationService(AppNotificationRepository notificationRepository,
                                  NotificationStateRepository stateRepository,
                                  org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.notificationRepository = notificationRepository;
        this.stateRepository = stateRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional("postgresTransactionManager")
    public Optional<NotificationResponse> createJournalReminder(Long patientId, String message,
                                                                 String actionUrl, String dedupeKey) {
        if (notificationRepository.findByDedupeKey(dedupeKey).isPresent()) {
            return Optional.empty();
        }
        AppNotification notification = new AppNotification(
                NotificationRecipientType.PATIENT, patientId, NotificationType.JOURNAL_REMINDER,
                "Nhắc ghi nhật ký hôm nay", message, "JOURNAL", null, actionUrl, dedupeKey);
        notification = notificationRepository.save(notification);
        NotificationResponse response = toResponse(notification, null);
        eventPublisher.publishEvent(new NotificationCreatedEvent(
                NotificationRecipientType.PATIENT, patientId, response));
        return Optional.of(response);
    }

    @Transactional("postgresTransactionManager")
    public Optional<NotificationResponse> createMedicationReminder(Long patientId, Long reminderId,
                                                                    String title, String message,
                                                                    String dedupeKey) {
        return createMedicationReminder(patientId, reminderId, title, message, dedupeKey, null);
    }

    @Transactional("postgresTransactionManager")
    public Optional<NotificationResponse> createMedicationReminder(Long patientId, Long reminderId,
                                                                    String title, String message,
                                                                    String dedupeKey, String scheduledTime) {
        if (notificationRepository.findByDedupeKey(dedupeKey).isPresent()) {
            return Optional.empty();
        }
        AppNotification notification = new AppNotification(
                NotificationRecipientType.PATIENT, patientId, NotificationType.MEDICATION_REMINDER,
                title, message, "MEDICATION_REMINDER", reminderId, "/medications", dedupeKey, scheduledTime);
        notification = notificationRepository.save(notification);
        NotificationResponse response = toResponse(notification, null);
        eventPublisher.publishEvent(new NotificationCreatedEvent(
                NotificationRecipientType.PATIENT, patientId, response));
        return Optional.of(response);
    }

    /**
     * Lưu cảnh báo bảo mật cho từng lần nhập sai mật khẩu của bệnh nhân. Không dùng
     * dedupeKey vì mỗi lần thử là một sự kiện độc lập cần được giữ lại để chủ tài khoản
     * có thể đối chiếu với lịch sử đăng nhập.
     */
    @Transactional("postgresTransactionManager")
    public NotificationResponse createFailedLoginAlert(Long patientId, String deviceLabel,
                                                        String ipAddress, boolean accountLocked) {
        String title = accountLocked
                ? "Tài khoản tạm thời bị khóa"
                : "Đăng nhập không thành công";
        String message = accountLocked
                ? "Tài khoản của bạn vừa bị khóa tạm thời sau nhiều lần nhập sai mật khẩu từ %s (IP %s)."
                        .formatted(deviceLabel, ipAddress)
                : "Phát hiện một lần nhập sai mật khẩu từ %s (IP %s). Nếu không phải bạn, hãy đổi mật khẩu ngay."
                        .formatted(deviceLabel, ipAddress);
        AppNotification notification = new AppNotification(
                NotificationRecipientType.PATIENT, patientId, NotificationType.SYSTEM,
                title, message, "LOGIN_SECURITY", null, "/login-history", null);
        notification = notificationRepository.save(notification);
        NotificationResponse response = toResponse(notification, null);
        eventPublisher.publishEvent(new NotificationCreatedEvent(
                NotificationRecipientType.PATIENT, patientId, response));
        return response;
    }
    
    @Transactional("postgresTransactionManager")
    public Optional<NotificationResponse> createAuditAnomalyAlert(String title, String message, String dedupeKey) {
        if (notificationRepository.findByDedupeKey(dedupeKey).isPresent()) {
            return Optional.empty();
        }
        AppNotification notification = new AppNotification(
                NotificationRecipientType.ADMIN, null, NotificationType.SYSTEM,
                title, message, "AUDIT_ANOMALY", null, "/cms/audit-logs", dedupeKey);
        notification = notificationRepository.save(notification);
        NotificationResponse response = toResponse(notification, null);
        eventPublisher.publishEvent(new NotificationCreatedEvent(
                NotificationRecipientType.ADMIN, null, response));
        return Optional.of(response);
    }
    /** Notification triage là broadcast cho CMS Admin và được lưu cùng transaction với alert. */
    @Transactional("postgresTransactionManager")
    public Optional<NotificationResponse> recordTriageAlert(TriageAlertEvent event) {
        String dedupeKey = "TRIAGE_ALERT:" + event.id();
        if (notificationRepository.findByDedupeKey(dedupeKey).isPresent()) {
            return Optional.empty();
        }
        AppNotification notification = new AppNotification(
                NotificationRecipientType.ADMIN, null, NotificationType.TRIAGE_ALERT,
                "Cảnh báo triage mới", event.messageContent(), "TRIAGE_ALERT", event.id(),
                "/cms/triage-alerts", dedupeKey);
        notification = notificationRepository.save(notification);
        NotificationResponse response = toResponse(notification, null);
        eventPublisher.publishEvent(new NotificationCreatedEvent(
                NotificationRecipientType.ADMIN, null, response));
        return Optional.of(response);
    }

    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public NotificationPageResponse listPatient(Long patientId, NotificationType type,
                                                boolean unreadOnly, Pageable pageable) {
        return list(notificationRepository.findVisibleForPatient(patientId, type, unreadOnly, pageable),
                NotificationRecipientType.PATIENT, patientId,
                notificationRepository.countUnreadForPatient(patientId));
    }

    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public NotificationPageResponse listAdmin(Long adminId, NotificationType type,
                                              boolean unreadOnly, Pageable pageable) {
        return list(notificationRepository.findVisibleForAdmin(adminId, type, unreadOnly, pageable),
                NotificationRecipientType.ADMIN, adminId,
                notificationRepository.countUnreadForAdmin(adminId));
    }

    @Transactional("postgresTransactionManager")
    public void markRead(Long notificationId, NotificationRecipientType type, Long recipientId) {
        AppNotification notification = visibleNotification(notificationId, type, recipientId);
        NotificationState state = stateRepository
                .findByNotificationIdAndRecipientTypeAndRecipientId(notificationId, type, recipientId)
                .orElseGet(() -> new NotificationState(notification, type, recipientId));
        state.setReadAt(state.getReadAt() == null ? Instant.now() : state.getReadAt());
        stateRepository.save(state);
    }

    @Transactional("postgresTransactionManager")
    public void markUnread(Long notificationId, NotificationRecipientType type, Long recipientId) {
        visibleNotification(notificationId, type, recipientId);
        stateRepository.findByNotificationIdAndRecipientTypeAndRecipientId(notificationId, type, recipientId)
                .ifPresent(state -> {
                    state.setReadAt(null);
                    stateRepository.save(state);
                });
    }

    @Transactional("postgresTransactionManager")
    public void markAllRead(NotificationRecipientType type, Long recipientId) {
        stateRepository.markAllRead(type.name(), recipientId, Instant.now());
    }

    @Transactional("postgresTransactionManager")
    public void delete(Long notificationId, NotificationRecipientType type, Long recipientId) {
        AppNotification notification = visibleNotification(notificationId, type, recipientId);
        NotificationState state = stateRepository
                .findByNotificationIdAndRecipientTypeAndRecipientId(notificationId, type, recipientId)
                .orElseGet(() -> new NotificationState(notification, type, recipientId));
        state.setDeletedAt(state.getDeletedAt() == null ? Instant.now() : state.getDeletedAt());
        stateRepository.save(state);
    }

    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public long unreadPatient(Long patientId) {
        return notificationRepository.countUnreadForPatient(patientId);
    }

    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public long unreadAdmin(Long adminId) {
        return notificationRepository.countUnreadForAdmin(adminId);
    }

    private NotificationPageResponse list(Page<AppNotification> page, NotificationRecipientType type,
                                           Long recipientId, long unreadCount) {
        List<Long> ids = page.getContent().stream().map(AppNotification::getId).toList();
        Map<Long, NotificationState> states = new HashMap<>();
        if (!ids.isEmpty()) {
            stateRepository.findByNotificationIdInAndRecipientTypeAndRecipientId(ids, type, recipientId)
                    .forEach(state -> states.put(state.getNotification().getId(), state));
        }
        List<NotificationResponse> items = page.getContent().stream()
                .map(notification -> toResponse(notification, Optional.ofNullable(states.get(notification.getId()))
                        .map(NotificationState::getReadAt).orElse(null)))
                .toList();
        return new NotificationPageResponse(items, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), unreadCount);
    }

    private AppNotification visibleNotification(Long notificationId, NotificationRecipientType type,
                                                Long recipientId) {
        AppNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông báo"));
        boolean visible = type == NotificationRecipientType.PATIENT
                ? type == notification.getRecipientType() && recipientId.equals(notification.getRecipientId())
                : notification.getRecipientType() == NotificationRecipientType.ADMIN
                && notification.getRecipientId() == null;
        if (!visible) {
            throw new org.springframework.security.access.AccessDeniedException("Không có quyền với thông báo này");
        }
        return notification;
    }

    private NotificationResponse toResponse(AppNotification notification, Instant readAt) {
        return new NotificationResponse(notification.getId(), notification.getNotificationType().name(),
                notification.getTitle(), notification.getMessage(), notification.getSourceType(),
                notification.getSourceId(), notification.getActionUrl(), notification.getCreatedAt(), readAt,
                notification.getScheduledTime());
    }
}
