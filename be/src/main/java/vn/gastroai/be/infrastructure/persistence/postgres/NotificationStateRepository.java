package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.gastroai.be.domain.notification.NotificationRecipientType;
import vn.gastroai.be.domain.notification.NotificationState;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificationStateRepository extends JpaRepository<NotificationState, Long> {

    Optional<NotificationState> findByNotificationIdAndRecipientTypeAndRecipientId(
            Long notificationId, NotificationRecipientType recipientType, Long recipientId);

    List<NotificationState> findByNotificationIdInAndRecipientTypeAndRecipientId(
            Collection<Long> notificationIds, NotificationRecipientType recipientType, Long recipientId);

    @Modifying
    @Query(value = """
            INSERT INTO notification_states
                (notification_id, recipient_type, recipient_id, read_at)
            SELECT n.id, :recipientType, :recipientId, :readAt
            FROM notifications n
            WHERE n.recipient_type = :recipientType
              AND ((:recipientType = 'PATIENT' AND n.recipient_id = :recipientId)
                   OR (:recipientType = 'ADMIN' AND n.recipient_id IS NULL))
            ON CONFLICT (notification_id, recipient_type, recipient_id)
            DO UPDATE SET read_at = COALESCE(notification_states.read_at, EXCLUDED.read_at)
            """, nativeQuery = true)
    int markAllRead(@Param("recipientType") String recipientType,
                    @Param("recipientId") Long recipientId,
                    @Param("readAt") Instant readAt);

    /**
     * Ca cap cuu da dong: danh dau da doc thong bao broadcast cua ca do cho 1 Admin.
     * Bang admins nam o MySQL nen khong JOIN duoc - service lay danh sach id roi goi tung nguoi.
     */
    @Modifying
    @Query(value = """
            INSERT INTO notification_states
                (notification_id, recipient_type, recipient_id, read_at)
            SELECT n.id, 'ADMIN', :adminId, :readAt
            FROM notifications n
            WHERE n.recipient_type = 'ADMIN' AND n.recipient_id IS NULL
              AND n.source_type = 'TRIAGE_ALERT' AND n.source_id = :alertId
            ON CONFLICT (notification_id, recipient_type, recipient_id)
            DO UPDATE SET read_at = COALESCE(notification_states.read_at, EXCLUDED.read_at)
            """, nativeQuery = true)
    int markTriageAlertAdminNotificationsRead(@Param("alertId") Long alertId,
                                              @Param("adminId") Long adminId,
                                              @Param("readAt") Instant readAt);

    /** Ca cap cuu da dong: danh dau da doc thong bao hoi tham "Ban co on khong?" cua benh nhan. */
    @Modifying
    @Query(value = """
            INSERT INTO notification_states
                (notification_id, recipient_type, recipient_id, read_at)
            SELECT n.id, 'PATIENT', n.recipient_id, :readAt
            FROM notifications n
            WHERE n.recipient_type = 'PATIENT'
              AND n.source_type = 'TRIAGE_ALERT' AND n.source_id = :alertId
            ON CONFLICT (notification_id, recipient_type, recipient_id)
            DO UPDATE SET read_at = COALESCE(notification_states.read_at, EXCLUDED.read_at)
            """, nativeQuery = true)
    int markTriageAlertPatientNotificationsRead(@Param("alertId") Long alertId, @Param("readAt") Instant readAt);
}
