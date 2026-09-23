package vn.gastroai.be.infrastructure.persistence.mysql;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.admin.AdminLoginHistory;
import vn.gastroai.be.domain.auth.LoginOutcome;

import java.time.Instant;

public interface AdminLoginHistoryRepository
        extends JpaRepository<AdminLoginHistory, Long> {

    /** Dùng bởi CmsAuthController — lịch sử đăng nhập của 1 Admin, mới nhất trước. */
    Page<AdminLoginHistory> findByAdminId(Long adminId, Pageable pageable);

    /** Tương tự PatientLoginHistoryRepository — đếm thất bại 7 ngày gần nhất, không phân trang. */
    long countByAdminIdAndOutcomeNotAndAttemptedAtAfter(
            Long adminId, LoginOutcome outcome, Instant after);

    // Lưu ý: chưa có scheduler nào gọi phương thức này (khác PatientLoginHistoryRepository,
    // có LoginHistoryCleanupScheduler dọn định kỳ) — lịch sử đăng nhập Admin hiện tích luỹ
    // vô thời hạn, không tự dọn.
    long deleteByAttemptedAtBefore(Instant cutoff);
}