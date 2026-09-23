package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.auth.LoginOutcome;
import vn.gastroai.be.domain.auth.PatientLoginHistory;

import java.time.Instant;

public interface PatientLoginHistoryRepository extends JpaRepository<PatientLoginHistory, Long> {
    /** UC0007 - phân trang lịch sử đăng nhập của 1 bệnh nhân, MeController sort mới nhất trước. */
    Page<PatientLoginHistory> findByPatientId(Long patientId, Pageable pageable);

    /**
     * Đếm số lần KHÔNG thành công (FAILURE/BLOCKED) trong khoảng thời gian gần đây, tính trên
     * TOÀN BỘ lịch sử — không chỉ trang đang xem — để MeController làm cảnh báo bảo mật chính
     * xác, tránh trường hợp lần thất bại nằm ở trang 2 mà trang 1 không cảnh báo gì.
     */
    long countByPatientIdAndOutcomeNotAndAttemptedAtAfter(
            Long patientId, LoginOutcome outcome, Instant after);

    /** Dùng bởi LoginHistoryCleanupService — dọn log cũ hơn historyRetentionDays. */
    long deleteByAttemptedAtBefore(Instant cutoff);
}
