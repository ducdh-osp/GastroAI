package vn.gastroai.be.infrastructure.persistence.mysql;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.admin.AdminLoginHistory;

import java.time.Instant;

public interface AdminLoginHistoryRepository
        extends JpaRepository<AdminLoginHistory, Long> {

    long deleteByAttemptedAtBefore(Instant cutoff);
}