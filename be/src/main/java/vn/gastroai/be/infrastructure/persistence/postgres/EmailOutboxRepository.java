package vn.gastroai.be.infrastructure.persistence.postgres;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.gastroai.be.domain.notification.EmailOutbox;
import vn.gastroai.be.domain.notification.EmailOutboxStatus;
import vn.gastroai.be.domain.notification.EmailTemplateCode;

import java.time.Instant;
import java.util.Optional;

public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, Long> {

    boolean existsByDedupeKey(String dedupeKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailOutbox> findFirstByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAscIdAsc(
            EmailOutboxStatus status, Instant now);

    @Modifying
    @Query("""
            update EmailOutbox e
               set e.status = :cancelled, e.updatedAt = :now
             where e.patientId = :patientId
               and e.templateCode = :templateCode
               and e.status = :pending
            """)
    int cancelPendingForPatientAndTemplate(
            @Param("patientId") Long patientId,
            @Param("templateCode") EmailTemplateCode templateCode,
            @Param("pending") EmailOutboxStatus pending,
            @Param("cancelled") EmailOutboxStatus cancelled,
            @Param("now") Instant now);
}
