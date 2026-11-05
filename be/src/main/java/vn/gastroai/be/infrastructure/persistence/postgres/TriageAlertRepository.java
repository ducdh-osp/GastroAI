package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.domain.triage.TriageAlert;
import vn.gastroai.be.domain.triage.TriageAlertOutcome;
import vn.gastroai.be.domain.triage.TriageAlertStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import vn.gastroai.be.infrastructure.persistence.postgres.projection.TriageStatusStatProjection;

public interface TriageAlertRepository extends JpaRepository<TriageAlert, Long> {

    interface PatientAlertCount {
        Long getPatientId();
        long getTotal();
    }

    long countByStatus(TriageAlertStatus status);
    List<TriageAlert> findByOccurredAtBetween(Instant startDate, Instant endDate);

    @Query(value = """
            SELECT status AS status, count(*) AS count
            FROM triage_alerts
            WHERE occurred_at >= :startDate AND occurred_at <= :endDate
            GROUP BY status
            """, nativeQuery = true)
    List<TriageStatusStatProjection> findStatusStats(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Danh sach CMS: NEW truoc, roi IN_PROGRESS, roi RESOLVED; trong nhom theo hoat dong
     * gan nhat. Pageable truyen vao KHONG duoc co Sort (thu tu da co san trong query).
     */
    @Query(value = "SELECT a FROM TriageAlert a JOIN FETCH a.patient WHERE a.status IN :statuses "
            + "ORDER BY CASE a.status "
            + "WHEN vn.gastroai.be.domain.triage.TriageAlertStatus.NEW THEN 0 "
            + "WHEN vn.gastroai.be.domain.triage.TriageAlertStatus.IN_PROGRESS THEN 1 "
            + "ELSE 2 END, "
            + "COALESCE(a.lastRepeatedAt, a.occurredAt) DESC, a.id DESC",
            countQuery = "SELECT count(a) FROM TriageAlert a WHERE a.status IN :statuses")
    Page<TriageAlert> findPageByStatuses(
            @Param("statuses") Collection<TriageAlertStatus> statuses, Pageable pageable);

    /** Buoc 1 gop trung: canh bao chua dong co hoat dong gan nhat sau :after (dung Pageable 1 dong). */
    @Query("SELECT a FROM TriageAlert a WHERE a.patient.id = :patientId "
            + "AND a.status <> vn.gastroai.be.domain.triage.TriageAlertStatus.RESOLVED "
            + "AND COALESCE(a.lastRepeatedAt, a.occurredAt) > :after "
            + "ORDER BY COALESCE(a.lastRepeatedAt, a.occurredAt) DESC, a.id DESC")
    List<TriageAlert> findRecentOpenForPatient(
            @Param("patientId") Long patientId,
            @Param("after") Instant after,
            Pageable pageable);

    /** Buoc 2 gop trung: canh bao gan nhat cua benh nhan (bat ke trang thai). */
    Optional<TriageAlert> findFirstByPatient_IdOrderByOccurredAtDescIdDesc(Long patientId);

    /** Cac canh bao cua benh nhan da duoc nhan vien nhan it nhat 1 tin. */
    @Query("SELECT a FROM TriageAlert a WHERE a.patient.id = :patientId AND EXISTS ("
            + "SELECT m.id FROM TriageAlertMessage m WHERE m.alertId = a.id AND m.senderType <> 'PATIENT')")
    List<TriageAlert> findWithStaffMessagesForPatient(@Param("patientId") Long patientId);

    @Query("SELECT a.patient.id AS patientId, count(a) AS total FROM TriageAlert a "
            + "WHERE a.patient.id IN :patientIds "
            + "AND a.outcome = vn.gastroai.be.domain.triage.TriageAlertOutcome.FALSE_ALARM "
            + "GROUP BY a.patient.id")
    List<PatientAlertCount> countFalseAlarmsByPatient(@Param("patientIds") Collection<Long> patientIds);

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
     * Tra lai canh bao ve NEW - chi nguoi da tiep nhan (claimedById/claimedByType) duoc goi.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("UPDATE TriageAlert a SET a.status = vn.gastroai.be.domain.triage.TriageAlertStatus.NEW, "
            + "a.claimedById = null, a.claimedByType = null, a.claimedAt = null, a.updatedAt = :now "
            + "WHERE a.id = :id AND a.status = vn.gastroai.be.domain.triage.TriageAlertStatus.IN_PROGRESS "
            + "AND a.claimedById = :claimerId AND a.claimedByType = :claimerType")
    int releaseIfClaimedBy(
            @Param("id") Long id,
            @Param("claimerId") Long claimerId,
            @Param("claimerType") String claimerType,
            @Param("now") Instant now);

    /** ADMIN tra lai ho canh bao dang IN_PROGRESS cua nguoi khac. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("UPDATE TriageAlert a SET a.status = vn.gastroai.be.domain.triage.TriageAlertStatus.NEW, "
            + "a.claimedById = null, a.claimedByType = null, a.claimedAt = null, a.updatedAt = :now "
            + "WHERE a.id = :id AND a.status = vn.gastroai.be.domain.triage.TriageAlertStatus.IN_PROGRESS")
    int releaseAsAdmin(
            @Param("id") Long id,
            @Param("now") Instant now);

    /**
     * Danh dau RESOLVED (kem outcome/ghi chu) chi khi nguoi goi (resolverId/resolverType) dung la
     * nguoi da tiep nhan canh bao nay (claimedById/claimedByType) VA canh bao dang IN_PROGRESS.
     * Day la duong di thuong, danh cho Bac si/Admin tu dong ho canh bao minh tiep nhan.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("UPDATE TriageAlert a SET a.status = vn.gastroai.be.domain.triage.TriageAlertStatus.RESOLVED, "
            + "a.outcome = :outcome, a.resolutionNote = :note, "
            + "a.resolvedAt = :now, a.resolvedById = :resolverId, a.resolvedByType = :resolverType, "
            + "a.updatedAt = :now "
            + "WHERE a.id = :id AND a.status = vn.gastroai.be.domain.triage.TriageAlertStatus.IN_PROGRESS "
            + "AND a.claimedById = :resolverId AND a.claimedByType = :resolverType")
    int resolveIfClaimedBy(
            @Param("id") Long id,
            @Param("resolverId") Long resolverId,
            @Param("resolverType") String resolverType,
            @Param("outcome") TriageAlertOutcome outcome,
            @Param("note") String note,
            @Param("now") Instant now);

    /**
     * Quyen "dong ho" rieng cho ADMIN: khong can la nguoi da tiep nhan, chi can canh bao
     * dang IN_PROGRESS. claimedById/claimedByType giu nguyen de van biet ai thuc su xu ly,
     * chi resolvedByType duoc ghi cung la 'ADMIN'.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("UPDATE TriageAlert a SET a.status = vn.gastroai.be.domain.triage.TriageAlertStatus.RESOLVED, "
            + "a.outcome = :outcome, a.resolutionNote = :note, "
            + "a.resolvedAt = :now, a.resolvedById = :adminId, a.resolvedByType = 'ADMIN', "
            + "a.updatedAt = :now "
            + "WHERE a.id = :id AND a.status = vn.gastroai.be.domain.triage.TriageAlertStatus.IN_PROGRESS")
    int resolveAsAdminOverride(
            @Param("id") Long id,
            @Param("adminId") Long adminId,
            @Param("outcome") TriageAlertOutcome outcome,
            @Param("note") String note,
            @Param("now") Instant now);
}
