package vn.gastroai.be.domain.triage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "triage_alert_actions")
@Getter
@Setter
@NoArgsConstructor
public class TriageAlertAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_id", nullable = false)
    private Long alertId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    private TriageAlertActionType actionType;

    // null khi he thong/benh nhan tu sinh (CREATED, REPEATED...)
    @Column(name = "actor_type", length = 20)
    private String actorType;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(length = 1000)
    private String detail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public TriageAlertAction(Long alertId, TriageAlertActionType actionType,
            String actorType, Long actorId, String detail) {
        this.alertId = alertId;
        this.actionType = actionType;
        this.actorType = actorType;
        this.actorId = actorId;
        this.detail = detail;
    }
}
