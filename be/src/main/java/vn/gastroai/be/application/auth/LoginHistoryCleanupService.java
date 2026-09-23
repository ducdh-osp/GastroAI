package vn.gastroai.be.application.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientLoginHistoryRepository;
import java.time.Instant;

/** Được LoginHistoryCleanupScheduler gọi hằng đêm — dọn lịch sử đăng nhập Bệnh nhân quá cũ. */
@Service
public class LoginHistoryCleanupService {
    private final PatientLoginHistoryRepository patients;
    private final LoginSecurityPolicy policy;

    public LoginHistoryCleanupService(PatientLoginHistoryRepository patients,
                                      LoginSecurityPolicy policy) {
        this.patients = patients;
        this.policy = policy;
    }

    /** Xoá mọi bản ghi cũ hơn LoginSecurityPolicy.historyRetentionDays (mặc định 90 ngày). */
    @Transactional("postgresTransactionManager")
    public long cleanupPatients() {
        Instant cutoff = Instant.now().minus(java.time.Duration.ofDays(policy.historyRetentionDays()));
        return patients.deleteByAttemptedAtBefore(cutoff);
    }
}
