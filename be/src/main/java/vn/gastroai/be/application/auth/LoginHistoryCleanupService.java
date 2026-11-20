package vn.gastroai.be.application.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminLoginHistoryRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientLoginHistoryRepository;
import java.time.Instant;

/** Được LoginHistoryCleanupScheduler gọi hằng đêm — dọn lịch sử đăng nhập quá cũ của bệnh nhân và tài khoản CMS. */
@Service
public class LoginHistoryCleanupService {
    private final PatientLoginHistoryRepository patients;
    private final AdminLoginHistoryRepository admins;
    private final LoginSecurityPolicy policy;

    public LoginHistoryCleanupService(PatientLoginHistoryRepository patients,
                                      AdminLoginHistoryRepository admins,
                                      LoginSecurityPolicy policy) {
        this.patients = patients;
        this.admins = admins;
        this.policy = policy;
    }

    /** Xoá mọi bản ghi cũ hơn LoginSecurityPolicy.historyRetentionDays (mặc định 90 ngày). */
    @Transactional("postgresTransactionManager")
    public long cleanupPatients() {
        return patients.deleteByAttemptedAtBefore(cutoff());
    }

    /** Cùng thời hạn lưu với bệnh nhân. Hai bảng nằm ở hai cơ sở dữ liệu nên mỗi bên một giao dịch riêng. */
    @Transactional("mysqlTransactionManager")
    public long cleanupAdmins() {
        return admins.deleteByAttemptedAtBefore(cutoff());
    }

    private Instant cutoff() {
        return Instant.now().minus(java.time.Duration.ofDays(policy.historyRetentionDays()));
    }
}