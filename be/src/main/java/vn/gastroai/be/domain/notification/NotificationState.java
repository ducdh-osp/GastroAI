package vn.gastroai.be.domain.notification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "notification_states", uniqueConstraints = @UniqueConstraint(
        name = "uq_notification_state_recipient",
        columnNames = {"notification_id", "recipient_type", "recipient_id"}))
@Getter
@Setter
@NoArgsConstructor
public class NotificationState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private AppNotification notification;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false, length = 20)
    private NotificationRecipientType recipientType;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public NotificationState(AppNotification notification, NotificationRecipientType recipientType,
                             Long recipientId) {
        this.notification = notification;
        this.recipientType = recipientType;
        this.recipientId = recipientId;
    }
}
