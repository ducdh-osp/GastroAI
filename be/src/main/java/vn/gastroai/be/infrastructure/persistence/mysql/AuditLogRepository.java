package vn.gastroai.be.infrastructure.persistence.mysql;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.audit.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}