package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.gastroai.be.domain.notification.AppNotification;
import vn.gastroai.be.domain.notification.NotificationType;

import java.util.Optional;

public interface AppNotificationRepository extends JpaRepository<AppNotification, Long> {

    Optional<AppNotification> findByDedupeKey(String dedupeKey);

    @Query(value = """
            select n from AppNotification n
            where n.recipientType = vn.gastroai.be.domain.notification.NotificationRecipientType.PATIENT
              and n.recipientId = :recipientId
              and (:notificationType is null or n.notificationType = :notificationType)
              and (:unreadOnly = false or not exists (
                  select unreadState.id from NotificationState unreadState
                  where unreadState.notification.id = n.id
                    and unreadState.recipientType = vn.gastroai.be.domain.notification.NotificationRecipientType.PATIENT
                    and unreadState.recipientId = :recipientId
                    and unreadState.readAt is not null))
              and not exists (
                  select s.id from NotificationState s
                  where s.notification.id = n.id
                    and s.recipientType = vn.gastroai.be.domain.notification.NotificationRecipientType.PATIENT
                    and s.recipientId = :recipientId
                    and s.deletedAt is not null)
            order by n.createdAt desc
            """,
            countQuery = """
            select count(n) from AppNotification n
            where n.recipientType = vn.gastroai.be.domain.notification.NotificationRecipientType.PATIENT
              and n.recipientId = :recipientId
              and (:notificationType is null or n.notificationType = :notificationType)
              and (:unreadOnly = false or not exists (
                  select unreadState.id from NotificationState unreadState
                  where unreadState.notification.id = n.id
                    and unreadState.recipientType = vn.gastroai.be.domain.notification.NotificationRecipientType.PATIENT
                    and unreadState.recipientId = :recipientId
                    and unreadState.readAt is not null))
              and not exists (
                  select s.id from NotificationState s
                  where s.notification.id = n.id
                    and s.recipientType = vn.gastroai.be.domain.notification.NotificationRecipientType.PATIENT
                    and s.recipientId = :recipientId
                    and s.deletedAt is not null)
            """)
    Page<AppNotification> findVisibleForPatient(@Param("recipientId") Long recipientId,
                                                 @Param("notificationType") NotificationType notificationType,
                                                 @Param("unreadOnly") boolean unreadOnly,
                                                 Pageable pageable);

    @Query(value = """
            select n from AppNotification n
            where n.recipientType = vn.gastroai.be.domain.notification.NotificationRecipientType.ADMIN
              and n.recipientId is null
              and (:notificationType is null or n.notificationType = :notificationType)
              and (:unreadOnly = false or not exists (
                  select unreadState.id from NotificationState unreadState
                  where unreadState.notification.id = n.id
                    and unreadState.recipientType = vn.gastroai.be.domain.notification.NotificationRecipientType.ADMIN
                    and unreadState.recipientId = :recipientId
                    and unreadState.readAt is not null))
              and not exists (
                  select s.id from NotificationState s
                  where s.notification.id = n.id
                    and s.recipientType = vn.gastroai.be.domain.notification.NotificationRecipientType.ADMIN
                    and s.recipientId = :recipientId
                    and s.deletedAt is not null)
            order by n.createdAt desc
            """,
            countQuery = """
            select count(n) from AppNotification n
            where n.recipientType = vn.gastroai.be.domain.notification.NotificationRecipientType.ADMIN
              and n.recipientId is null
              and (:notificationType is null or n.notificationType = :notificationType)
              and (:unreadOnly = false or not exists (
                  select unreadState.id from NotificationState unreadState
                  where unreadState.notification.id = n.id
                    and unreadState.recipientType = vn.gastroai.be.domain.notification.NotificationRecipientType.ADMIN
                    and unreadState.recipientId = :recipientId
                    and unreadState.readAt is not null))
              and not exists (
                  select s.id from NotificationState s
                  where s.notification.id = n.id
                    and s.recipientType = vn.gastroai.be.domain.notification.NotificationRecipientType.ADMIN
                    and s.recipientId = :recipientId
                    and s.deletedAt is not null)
            """)
    Page<AppNotification> findVisibleForAdmin(@Param("recipientId") Long recipientId,
                                               @Param("notificationType") NotificationType notificationType,
                                               @Param("unreadOnly") boolean unreadOnly,
                                               Pageable pageable);

    @Query("""
            select count(n) from AppNotification n
            where n.recipientType = vn.gastroai.be.domain.notification.NotificationRecipientType.PATIENT
              and n.recipientId = :recipientId
              and not exists (
                  select s.id from NotificationState s
                  where s.notification.id = n.id
                    and s.recipientType = vn.gastroai.be.domain.notification.NotificationRecipientType.PATIENT
                    and s.recipientId = :recipientId
                    and (s.readAt is not null or s.deletedAt is not null))
            """)
    long countUnreadForPatient(@Param("recipientId") Long recipientId);

    @Query("""
            select count(n) from AppNotification n
            where n.recipientType = vn.gastroai.be.domain.notification.NotificationRecipientType.ADMIN
              and n.recipientId is null
              and not exists (
                  select s.id from NotificationState s
                  where s.notification.id = n.id
                    and s.recipientType = vn.gastroai.be.domain.notification.NotificationRecipientType.ADMIN
                    and s.recipientId = :recipientId
                    and (s.readAt is not null or s.deletedAt is not null))
            """)
    long countUnreadForAdmin(@Param("recipientId") Long recipientId);
}
