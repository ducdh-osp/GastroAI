package vn.gastroai.be.infrastructure.persistence.mysql;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.admin.AdminLoginHistory;

import java.time.Instant;

public interface AdminLoginHistoryRepository
        extends JpaRepository<AdminLoginHistory, Long> {

    // Lưu ý: chưa có scheduler nào gọi phương thức này (khác PatientLoginHistoryRepository,
    // có LoginHistoryCleanupScheduler dọn định kỳ) — lịch sử đăng nhập Admin hiện tích luỹ
    // vô thời hạn, không tự dọn.
    long deleteByAttemptedAtBefore(Instant cutoff);
}