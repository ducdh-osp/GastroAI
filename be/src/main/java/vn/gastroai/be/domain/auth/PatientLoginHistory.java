package vn.gastroai.be.domain.auth;

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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "patient_login_history")
@Getter
@Setter
@NoArgsConstructor
public class PatientLoginHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoginOutcome outcome;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "user_agent", nullable = false, length = 1024)
    private String userAgent;

    @Column(name = "device_label", nullable = false)
    private String deviceLabel;

    public PatientLoginHistory(Patient patient, Instant attemptedAt, LoginOutcome outcome,
                               String failureReason, String ipAddress, String userAgent, String deviceLabel) {
        this.patient = patient;
        this.attemptedAt = attemptedAt;
        this.outcome = outcome;
        this.failureReason = failureReason;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.deviceLabel = deviceLabel;
    }
}
