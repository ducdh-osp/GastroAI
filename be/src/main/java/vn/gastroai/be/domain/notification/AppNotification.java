package vn.gastroai.be.domain.notification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class AppNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false, length = 20)
    private NotificationRecipientType recipientType;

    @Column(name = "recipient_id")
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 40)
    private NotificationType notificationType;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "source_type", length = 40)
    private String sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "action_url", length = 255)
    private String actionUrl;

    @Column(name = "dedupe_key", unique = true, length = 190)
    private String dedupeKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public AppNotification(NotificationRecipientType recipientType, Long recipientId,
                           NotificationType notificationType, String title, String message,
                           String sourceType, Long sourceId, String actionUrl, String dedupeKey) {
        this.recipientType = recipientType;
        this.recipientId = recipientId;
        this.notificationType = notificationType;
        this.title = title;
        this.message = message;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.actionUrl = actionUrl;
        this.dedupeKey = dedupeKey;
    }
}
