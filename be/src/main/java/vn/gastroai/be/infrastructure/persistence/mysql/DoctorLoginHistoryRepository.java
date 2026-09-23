package vn.gastroai.be.infrastructure.persistence.mysql;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.admin.DoctorLoginHistory;

import java.time.Instant;

public interface DoctorLoginHistoryRepository
        extends JpaRepository<DoctorLoginHistory, Long> {

    // Cùng tình trạng với AdminLoginHistoryRepository — chưa có scheduler dọn định kỳ.
    long deleteByAttemptedAtBefore(Instant cutoff);
}