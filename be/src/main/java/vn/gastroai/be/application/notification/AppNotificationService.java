package vn.gastroai.be.application.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.api.notification.NotificationPageResponse;
import vn.gastroai.be.api.notification.NotificationResponse;
import vn.gastroai.be.domain.notification.AppNotification;
import vn.gastroai.be.domain.notification.NotificationCreatedEvent;
import vn.gastroai.be.domain.notification.NotificationRecipientType;
import vn.gastroai.be.domain.notification.NotificationState;
import vn.gastroai.be.domain.notification.NotificationType;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.AppNotificationRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.NotificationStateRepository;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AppNotificationService {

    private static final Logger log = LoggerFactory.getLogger(AppNotificationService.class);

    private final AppNotificationRepository notificationRepository;
    private final NotificationStateRepository stateRepository;
    private final AdminRepository adminRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public AppNotificationService(AppNotificationRepository notificationRepository,
                                  NotificationStateRepository stateRepository,
                                  AdminRepository adminRepository,
                                  org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.notificationRepository = notificationRepository;
        this.stateRepository = stateRepository;
        this.adminRepository = adminRepository;
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

    @Transactional(value = "postgresTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public Optional<NotificationResponse> createMedicationReminder(Long patientId, Long reminderId,
                                                                    String title, String message,
                                                                    String dedupeKey) {
        return createMedicationReminder(patientId, reminderId, title, message, dedupeKey, null);
    }

    @Transactional(value = "postgresTransactionManager", propagation = Propagation.REQUIRES_NEW)
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
     * nhận được cảnh báo bảo mật riêng cho từng sự kiện.
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
                NotificationRecipientType.PATIENT, patientId, NotificationType.SECURITY_ALERT,
                title, message, "LOGIN_SECURITY", null, "/change-password", null);
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
    /**
     * Ca cap cuu vua dong (benh nhan bao an toan / Admin dong ca / bao nham): thong bao cua ca do
     * khong con viec gi phai lam nua -> danh dau da doc cho moi Admin va cho benh nhan.
     */
    @Transactional("postgresTransactionManager")
    public void markTriageAlertNotificationsRead(Long alertId) {
        Instant now = Instant.now();
        stateRepository.markTriageAlertPatientNotificationsRead(alertId, now);
        // Thong bao Admin la broadcast, trang thai doc luu theo tung Admin; bang admins o MySQL.
        // MySQL loi thi bo qua phan nay, KHONG de lam hong viec dong ca.
        List<Long> adminIds;
        try {
            adminIds = adminRepository.findAllIds();
        } catch (RuntimeException ex) {
            log.warn("Khong lay duoc danh sach Admin de danh dau da doc thong bao ca {}", alertId, ex);
            return;
        }
        for (Long adminId : adminIds) {
            stateRepository.markTriageAlertAdminNotificationsRead(alertId, adminId, now);
        }
    }

    /**
     * SCR-AD-13 buoc 1: hoi tham benh nhan ngay khi co canh bao. FE hien popup co 2 nut
     * [An toan] [Can ho tro]. reminder = lan nhac lai khi failsafe chay.
     */
    @Transactional("postgresTransactionManager")
    public Optional<NotificationResponse> recordTriageCheckIn(Long patientId, Long alertId, boolean reminder) {
        String dedupeKey = (reminder ? "TRIAGE_CHECK_IN_REMINDER:" : "TRIAGE_CHECK_IN:") + alertId;
        if (notificationRepository.findByDedupeKey(dedupeKey).isPresent()) {
            return Optional.empty();
        }
        String title = reminder ? "Nhắc lại: Bạn có ổn không?" : "Bạn có ổn không?";
        String message = "GastroAI vừa ghi nhận dấu hiệu cần chú ý trong điều bạn mô tả. "
                + "Bấm \"An toàn\" nếu bạn ổn, hoặc \"Cần hỗ trợ\" để nhân sự GastroAI liên hệ ngay.";
        AppNotification notification = new AppNotification(
                NotificationRecipientType.PATIENT, patientId, NotificationType.TRIAGE_CHECK_IN,
                title, message, "TRIAGE_ALERT", alertId, null, dedupeKey);
        notification = notificationRepository.save(notification);
        NotificationResponse response = toResponse(notification, null);
        eventPublisher.publishEvent(new NotificationCreatedEvent(
                NotificationRecipientType.PATIENT, patientId, response));
        return Optional.of(response);
    }

    /**
     * SCR-AD-13 buoc 3/4: bao TOAN BO Admin dang truc (broadcast, khong gan cho 1 nguoi). Lan dau
     * chong trung theo alertId; cac lan bao dong lai (failsafe, benh nhan bam Can ho tro lan nua)
     * luon gui moi.
     */
    @Transactional("postgresTransactionManager")
    public Optional<NotificationResponse> recordTriageNeedsIntervention(Long alertId, String patientName,
                                                                        String reason, boolean realert) {
        String dedupeKey = realert ? null : "TRIAGE_INTERVENTION:" + alertId;
        if (dedupeKey != null && notificationRepository.findByDedupeKey(dedupeKey).isPresent()) {
            return Optional.empty();
        }
        String name = patientName == null || patientName.isBlank() ? "Bệnh nhân" : patientName.trim();
        AppNotification notification = new AppNotification(
                NotificationRecipientType.ADMIN, null, NotificationType.TRIAGE_INTERVENTION,
                "Cần can thiệp: " + name, reason, "TRIAGE_ALERT", alertId,
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

    @Transactional("postgresTransactionManager")
    public void deleteAll(NotificationRecipientType type, Long recipientId) {
        stateRepository.deleteAll(type.name(), recipientId, Instant.now());
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
