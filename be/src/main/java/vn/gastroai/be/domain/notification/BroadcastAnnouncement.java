package vn.gastroai.be.domain.notification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * UC0075 - Admin gui thong bao broadcast toi toan bo hoac mot nhom benh nhan
 * (vd bao tri he thong, tai lieu y khoa moi).
 */
@Entity
@Table(name = "broadcast_announcements")
@Getter
@Setter
@NoArgsConstructor
public class BroadcastAnnouncement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "target_group", nullable = false, length = 40)
    private String targetGroup = "ALL";

    @Column(name = "action_url", length = 255)
    private String actionUrl;

    @Column(name = "created_by_admin_id")
    private Long createdByAdminId;

    @Column(name = "recipient_count", nullable = false)
    private int recipientCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public BroadcastAnnouncement(String title, String message, String targetGroup,
                                 String actionUrl, Long createdByAdminId) {
        this.title = title;
        this.message = message;
        this.targetGroup = targetGroup != null ? targetGroup : "ALL";
        this.actionUrl = actionUrl;
        this.createdByAdminId = createdByAdminId;
        this.recipientCount = 0;
        this.createdAt = Instant.now();
    }
}
