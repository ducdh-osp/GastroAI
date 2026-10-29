package vn.gastroai.be.infrastructure.persistence.mysql;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.gastroai.be.domain.audit.AuditAction;
import vn.gastroai.be.domain.audit.AuditLog;

import java.time.Instant;
import java.util.Collection;

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

    @Query("""
            SELECT COUNT(a) FROM AuditLog a
            WHERE a.actorType = :actorType
              AND a.actorId = :actorId
              AND a.action IN :actions
              AND a.occurredAt >= :since
            """)
    long countActions(@Param("actorType") String actorType,
                      @Param("actorId") Long actorId,
                      @Param("actions") Collection<AuditAction> actions,
                      @Param("since") Instant since);

    @Query("""
            SELECT COUNT(DISTINCT a.targetId) FROM AuditLog a
            WHERE a.actorType = :actorType
              AND a.actorId = :actorId
              AND a.action IN :actions
              AND a.occurredAt >= :since
            """)
    long countDistinctTargets(@Param("actorType") String actorType,
                              @Param("actorId") Long actorId,
                              @Param("actions") Collection<AuditAction> actions,
                              @Param("since") Instant since);

    @Query("""
            SELECT COUNT(DISTINCT a.ipAddress) FROM AuditLog a
            WHERE a.actorType = :actorType
              AND a.actorId = :actorId
              AND a.occurredAt >= :since
            """)
    long countDistinctIps(@Param("actorType") String actorType,
                          @Param("actorId") Long actorId,
                          @Param("since") Instant since);
}