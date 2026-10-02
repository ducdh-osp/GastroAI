package vn.gastroai.be.domain.triage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.gastroai.be.domain.auth.Patient;

import java.time.Instant;


@Entity
@Table(name = "triage_alerts")
@Getter
@Setter
@NoArgsConstructor
public class TriageAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "message_content", columnDefinition = "TEXT", nullable = false)
    private String messageContent;

    // JSON array of strings, giong cach ChatMessage.matchedGroups dang luu (xem V12).
    @Column(name = "matched_groups", columnDefinition = "TEXT")
    private String matchedGroups;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TriageAlertStatus status = TriageAlertStatus.NEW;

    @Column(name = "claimed_by_type", length = 20)
    private String claimedByType;

    @Column(name = "claimed_by_id")
    private Long claimedById;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by_type", length = 20)
    private String resolvedByType;

    @Column(name = "resolved_by_id")
    private Long resolvedById;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public TriageAlert(Patient patient, Long sessionId, Long messageId, String messageContent,
            String matchedGroups, Instant occurredAt) {
        this.patient = patient;
        this.sessionId = sessionId;
        this.messageId = messageId;
        this.messageContent = messageContent;
        this.matchedGroups = matchedGroups;
        this.occurredAt = occurredAt;
        this.status = TriageAlertStatus.NEW;
    }
}