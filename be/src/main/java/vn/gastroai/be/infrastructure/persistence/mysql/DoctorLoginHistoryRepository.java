package vn.gastroai.be.infrastructure.persistence.mysql;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.admin.DoctorLoginHistory;

import java.time.Instant;

public interface DoctorLoginHistoryRepository
        extends JpaRepository<DoctorLoginHistory, Long> {

    long deleteByAttemptedAtBefore(Instant cutoff);
}