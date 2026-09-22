package vn.gastroai.be.domain.admin;

import jakarta.persistence.*;
import lombok.*;
import vn.gastroai.be.domain.auth.LoginOutcome;
import java.time.Instant;

@Entity
@Table(name = "admin_login_history")
@Getter @Setter @NoArgsConstructor
public class AdminLoginHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false) private AdminAccount admin;
    @Column(name = "attempted_at", nullable = false) private Instant attemptedAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private LoginOutcome outcome;
    @Column(name = "failure_reason") private String failureReason;
    @Column(name = "ip_address", nullable = false, length = 45) private String ipAddress;
    @Column(name = "user_agent", nullable = false, length = 1024) private String userAgent;
    @Column(name = "device_label", nullable = false) private String deviceLabel;
    public AdminLoginHistory(AdminAccount admin, Instant attemptedAt, LoginOutcome outcome,
                             String failureReason, String ipAddress, String userAgent, String deviceLabel) {
        this.admin = admin; this.attemptedAt = attemptedAt; this.outcome = outcome;
        this.failureReason = failureReason; this.ipAddress = ipAddress;
        this.userAgent = userAgent; this.deviceLabel = deviceLabel;
    }
}
