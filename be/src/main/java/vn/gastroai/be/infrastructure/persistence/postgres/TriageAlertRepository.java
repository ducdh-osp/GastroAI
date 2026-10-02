package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.domain.triage.TriageAlert;
import vn.gastroai.be.domain.triage.TriageAlertStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TriageAlertRepository extends JpaRepository<TriageAlert, Long> {
    List<TriageAlert> findTop50ByOrderByOccurredAtDesc();
    Optional<TriageAlert> findFirstByPatient_IdAndStatusNotAndOccurredAtAfterOrderByOccurredAtDesc(
            Long patientId, TriageAlertStatus excludedStatus, Instant after);
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("UPDATE TriageAlert a SET a.status = vn.gastroai.be.domain.triage.TriageAlertStatus.IN_PROGRESS, "
            + "a.claimedById = :claimerId, a.claimedByType = :claimerType, "
            + "a.claimedAt = :now, a.updatedAt = :now "
            + "WHERE a.id = :id AND a.status = vn.gastroai.be.domain.triage.TriageAlertStatus.NEW")
    int claimIfNew(
            @Param("id") Long id,
            @Param("claimerId") Long claimerId,
            @Param("claimerType") String claimerType,
            @Param("now") Instant now);
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("UPDATE TriageAlert a SET a.status = vn.gastroai.be.domain.triage.TriageAlertStatus.RESOLVED, "
            + "a.resolvedAt = :now, a.updatedAt = :now "
            + "WHERE a.id = :id AND a.status = vn.gastroai.be.domain.triage.TriageAlertStatus.IN_PROGRESS")
    int resolveIfInProgress(@Param("id") Long id, @Param("now") Instant now);
}