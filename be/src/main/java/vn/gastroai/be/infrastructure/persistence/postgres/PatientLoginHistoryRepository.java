package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.auth.PatientLoginHistory;

import java.time.Instant;

public interface PatientLoginHistoryRepository extends JpaRepository<PatientLoginHistory, Long> {
    /** UC0007 - phân trang lịch sử đăng nhập của 1 bệnh nhân, MeController sort mới nhất trước. */
    Page<PatientLoginHistory> findByPatientId(Long patientId, Pageable pageable);

    /** Dùng bởi LoginHistoryCleanupService — dọn log cũ hơn historyRetentionDays. */
    long deleteByAttemptedAtBefore(Instant cutoff);
}
