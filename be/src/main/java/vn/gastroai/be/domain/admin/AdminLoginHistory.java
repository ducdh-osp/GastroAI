package vn.gastroai.be.domain.admin;

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
import vn.gastroai.be.domain.auth.LoginOutcome;

import java.time.Instant;

/**
 * 1 dòng = 1 lần thử đăng nhập của Admin — tương đương PatientLoginHistory bên Bệnh nhân
 * nhưng cho MySQL/Admin. Không dùng chung entity với PatientLoginHistory vì khác datasource
 * (JPA không cho @ManyToOne xuyên 2 EntityManagerFactory khác nhau).
 */
@Entity
@Table(name = "admin_login_history")
public class AdminLoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;

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

    public AdminLoginHistory() {
    }

    public AdminLoginHistory(
            Admin admin,
            Instant attemptedAt,
            LoginOutcome outcome,
            String failureReason,
            String ipAddress,
            String userAgent,
            String deviceLabel
    ) {
        this.admin = admin;
        this.attemptedAt = attemptedAt;
        this.outcome = outcome;
        this.failureReason = failureReason;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.deviceLabel = deviceLabel;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Admin getAdmin() {
        return admin;
    }

    public void setAdmin(Admin admin) {
        this.admin = admin;
    }

    public Instant getAttemptedAt() {
        return attemptedAt;
    }

    public void setAttemptedAt(Instant attemptedAt) {
        this.attemptedAt = attemptedAt;
    }

    public LoginOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(LoginOutcome outcome) {
        this.outcome = outcome;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getDeviceLabel() {
        return deviceLabel;
    }

    public void setDeviceLabel(String deviceLabel) {
        this.deviceLabel = deviceLabel;
    }
}