package vn.gastroai.be.application.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * UC0008 - Tham số chống brute-force, đọc từ application.yml (có default nếu không set).
 * Dùng chung cho cả Bệnh nhân (AuthService) và Admin/Bác sĩ (CmsAuthService) để nhất quán
 * chính sách khoá tài khoản trên toàn hệ thống.
 */
@Component
public class LoginSecurityPolicy {
    private final int maxFailedAttempts;
    private final Duration lockDuration;
    private final int historyRetentionDays;
    public LoginSecurityPolicy(
            @Value("${app.security.login.max-failed-attempts:5}") int maxFailedAttempts,
            @Value("${app.security.login.lock-duration-minutes:10}") long lockMinutes,
            @Value("${app.security.login.history-retention-days:90}") int historyRetentionDays) {
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockDuration = Duration.ofMinutes(lockMinutes);
        this.historyRetentionDays = historyRetentionDays;
    }

    public int maxFailedAttempts() {
        return maxFailedAttempts;
    }

    public Duration lockDuration() {
        return lockDuration;
    }

    public int historyRetentionDays() {
        return historyRetentionDays;
    }
}
