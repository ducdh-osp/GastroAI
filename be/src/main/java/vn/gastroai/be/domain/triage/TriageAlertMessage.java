package vn.gastroai.be.domain.triage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "triage_alert_messages")
@Getter
@Setter
@NoArgsConstructor
public class TriageAlertMessage {

    public static final String SENDER_PATIENT = "PATIENT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_id", nullable = false)
    private Long alertId;

    // PATIENT | DOCTOR | ADMIN - luu String giong TriageAlert.claimedByType
    @Column(name = "sender_type", nullable = false, length = 20)
    private String senderType;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public TriageAlertMessage(Long alertId, String senderType, Long senderId, String content) {
        this.alertId = alertId;
        this.senderType = senderType;
        this.senderId = senderId;
        this.content = content;
    }

    public boolean fromPatient() {
        return SENDER_PATIENT.equals(senderType);
    }
}
