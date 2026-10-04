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

    /**
     * Danh dau RESOLVED chi khi nguoi goi (resolverId/resolverType) dung la nguoi da
     * tiep nhan canh bao nay (claimedById/claimedByType) VA canh bao dang IN_PROGRESS.
     * Day la duong di thuong, danh cho Bac si/Admin tu dong ho canh bao minh tiep nhan.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("UPDATE TriageAlert a SET a.status = vn.gastroai.be.domain.triage.TriageAlertStatus.RESOLVED, "
            + "a.resolvedAt = :now, a.resolvedById = :resolverId, a.resolvedByType = :resolverType, "
            + "a.updatedAt = :now "
            + "WHERE a.id = :id AND a.status = vn.gastroai.be.domain.triage.TriageAlertStatus.IN_PROGRESS "
            + "AND a.claimedById = :resolverId AND a.claimedByType = :resolverType")
    int resolveIfClaimedBy(
            @Param("id") Long id,
            @Param("resolverId") Long resolverId,
            @Param("resolverType") String resolverType,
            @Param("now") Instant now);

    /**
     * Quyen "dong ho" rieng cho ADMIN: khong can la nguoi da tiep nhan, chi can canh bao
     * dang IN_PROGRESS. claimedById/claimedByType giu nguyen de van biet ai thuc su xu ly,
     * chi resolvedByType duoc ghi cung la 'ADMIN'.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("UPDATE TriageAlert a SET a.status = vn.gastroai.be.domain.triage.TriageAlertStatus.RESOLVED, "
            + "a.resolvedAt = :now, a.resolvedById = :adminId, a.resolvedByType = 'ADMIN', "
            + "a.updatedAt = :now "
            + "WHERE a.id = :id AND a.status = vn.gastroai.be.domain.triage.TriageAlertStatus.IN_PROGRESS")
    int resolveAsAdminOverride(
            @Param("id") Long id,
            @Param("adminId") Long adminId,
            @Param("now") Instant now);
}