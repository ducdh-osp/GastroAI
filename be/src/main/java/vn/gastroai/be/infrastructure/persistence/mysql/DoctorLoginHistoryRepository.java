package vn.gastroai.be.infrastructure.persistence.mysql;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.admin.DoctorLoginHistory;
import vn.gastroai.be.domain.auth.LoginOutcome;

import java.time.Instant;

public interface DoctorLoginHistoryRepository
        extends JpaRepository<DoctorLoginHistory, Long> {

    /** Dùng bởi CmsAuthController — lịch sử đăng nhập của 1 Bác sĩ, mới nhất trước. */
    Page<DoctorLoginHistory> findByDoctorId(Long doctorId, Pageable pageable);

    /** Tương tự PatientLoginHistoryRepository — đếm thất bại 7 ngày gần nhất, không phân trang. */
    long countByDoctorIdAndOutcomeNotAndAttemptedAtAfter(
            Long doctorId, LoginOutcome outcome, Instant after);

    // Cùng tình trạng với AdminLoginHistoryRepository — chưa có scheduler dọn định kỳ.
    long deleteByAttemptedAtBefore(Instant cutoff);
}