package vn.gastroai.be.infrastructure.persistence.mysql;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.gastroai.be.domain.audit.AuditAction;
import vn.gastroai.be.domain.audit.AuditLog;

import java.time.Instant;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:fromTime IS NULL OR a.occurredAt >= :fromTime)
              AND (:toTime IS NULL OR a.occurredAt < :toTime)
              AND (:action IS NULL OR a.action = :action)
              AND (:targetType IS NULL OR a.targetType = :targetType)
              AND (:keyword IS NULL
                   OR LOWER(a.actorEmail) LIKE :keyword
                   OR LOWER(a.detail) LIKE :keyword
                   OR a.targetId = :rawKeyword)
            """)
    Page<AuditLog> search(@Param("fromTime") Instant fromTime,
                          @Param("toTime") Instant toTime,
                          @Param("action") AuditAction action,
                          @Param("targetType") String targetType,
                          @Param("keyword") String keyword,
                          @Param("rawKeyword") String rawKeyword,
                          Pageable pageable);
}