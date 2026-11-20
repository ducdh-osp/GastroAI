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
import vn.gastroai.be.domain.triage.TriagePatientResponse;

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

    String STATUS = "vn.gastroai.be.domain.triage.TriageAlertStatus.";

    /**
     * Danh sach CMS (SCR-AD-13): "Can can thiep" truoc (trong do ca qua han failsafe len dau), roi
     * "He thong dang tu xu ly", roi "Da xu ly"; trong nhom theo hoat dong gan nhat.
     * Pageable truyen vao KHONG duoc co Sort (thu tu da co san trong query).
     */
    @Query(value = "SELECT a FROM TriageAlert a JOIN FETCH a.patient WHERE a.status IN :statuses "
            + "ORDER BY CASE a.status "
            + "WHEN " + STATUS + "NEEDS_INTERVENTION THEN 0 "
            + "WHEN " + STATUS + "AUTO_HANDLING THEN 1 "
            + "ELSE 2 END, "
            + "CASE WHEN a.failsafeAt IS NOT NULL THEN 0 ELSE 1 END, "
            + "COALESCE(a.lastRepeatedAt, a.occurredAt) DESC, a.id DESC",
            countQuery = "SELECT count(a) FROM TriageAlert a WHERE a.status IN :statuses")
    Page<TriageAlert> findPageByStatuses(
            @Param("statuses") Collection<TriageAlertStatus> statuses, Pageable pageable);

    /** Buoc 1 gop trung: canh bao con mo co hoat dong gan nhat (dung Pageable 1 dong). */
    @Query("SELECT a FROM TriageAlert a WHERE a.patient.id = :patientId "
            + "AND a.status <> " + STATUS + "RESOLVED "
            + "ORDER BY COALESCE(a.lastRepeatedAt, a.occurredAt) DESC, a.id DESC")
    List<TriageAlert> findLatestOpenForPatient(
            @Param("patientId") Long patientId,
            Pageable pageable);

    /** Buoc 2 gop trung: canh bao gan nhat cua benh nhan (bat ke trang thai). */
    Optional<TriageAlert> findFirstByPatient_IdOrderByOccurredAtDescIdDesc(Long patientId);

    @Query("SELECT a.patient.id AS patientId, count(a) AS total FROM TriageAlert a "
            + "WHERE a.patient.id IN :patientIds "
            + "AND a.outcome = vn.gastroai.be.domain.triage.TriageAlertOutcome.FALSE_ALARM "
            + "GROUP BY a.patient.id")
    List<PatientAlertCount> countFalseAlarmsByPatient(@Param("patientIds") Collection<Long> patientIds);

    // ===== Bo hen gio (TriageEscalationScheduler) =====

    /** Ca "dang tu xu ly" da qua moc ma benh nhan van im lang. */
    @Query("SELECT a.id FROM TriageAlert a WHERE a.status = " + STATUS + "AUTO_HANDLING "
            + "AND a.occurredAt <= :cutoff ORDER BY a.occurredAt")
    List<Long> findIdsDueForEscalation(@Param("cutoff") Instant cutoff);

    /** Ca "can can thiep" qua thoi han ma chua Admin nao ra tay, failsafe chua chay. */
    @Query("SELECT a.id FROM TriageAlert a WHERE a.status = " + STATUS + "NEEDS_INTERVENTION "
            + "AND a.acknowledgedAt IS NULL AND a.failsafeAt IS NULL "
            + "AND a.needsInterventionAt <= :cutoff ORDER BY a.needsInterventionAt")
    List<Long> findIdsDueForFailsafe(@Param("cutoff") Instant cutoff);

    /**
     * Chuyen "Can can thiep" - chi khi con dang tu xu ly (dieu kien trong WHERE chan 2 noi cung
     * chuyen: bo hen gio va benh nhan bam [Can ho tro] cung luc).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("UPDATE TriageAlert a SET a.status = " + STATUS + "NEEDS_INTERVENTION, "
            + "a.needsInterventionAt = :now, a.updatedAt = :now "
            + "WHERE a.id = :id AND a.status = " + STATUS + "AUTO_HANDLING")
    int escalateIfAuto(@Param("id") Long id, @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("UPDATE TriageAlert a SET a.failsafeAt = :now, a.updatedAt = :now "
            + "WHERE a.id = :id AND a.status = " + STATUS + "NEEDS_INTERVENTION "
            + "AND a.acknowledgedAt IS NULL AND a.failsafeAt IS NULL")
    int markFailsafeIfDue(@Param("id") Long id, @Param("now") Instant now);

    /**
     * Admin ra tay (ghi lien he / nhan tin) -> dung failsafe. Ca con dang tu xu ly thi chuyen luon
     * "Can can thiep" (Admin chu dong vao som).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("UPDATE TriageAlert a SET a.status = " + STATUS + "NEEDS_INTERVENTION, "
            + "a.needsInterventionAt = COALESCE(a.needsInterventionAt, :now), "
            + "a.acknowledgedAt = COALESCE(a.acknowledgedAt, :now), a.updatedAt = :now "
            + "WHERE a.id = :id AND a.status <> " + STATUS + "RESOLVED")
    int acknowledgeIfOpen(@Param("id") Long id, @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("UPDATE TriageAlert a SET a.patientResponse = :response, a.patientRespondedAt = :now, "
            + "a.updatedAt = :now WHERE a.id = :id")
    int recordPatientResponse(
            @Param("id") Long id,
            @Param("response") TriagePatientResponse response,
            @Param("now") Instant now);

    // ===== Tiep nhan "Toi dang lien he" (tu nguyen, de 2 Admin khong cung goi 1 nguoi than) =====

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("UPDATE TriageAlert a SET a.status = " + STATUS + "NEEDS_INTERVENTION, "
            + "a.needsInterventionAt = COALESCE(a.needsInterventionAt, :now), "
            + "a.acknowledgedAt = COALESCE(a.acknowledgedAt, :now), "
            + "a.claimedById = :claimerId, a.claimedByType = :claimerType, "
            + "a.claimedAt = :now, a.updatedAt = :now "
            + "WHERE a.id = :id AND a.status <> " + STATUS + "RESOLVED AND a.claimedById IS NULL")
    int claimIfOpen(
            @Param("id") Long id,
            @Param("claimerId") Long claimerId,
            @Param("claimerType") String claimerType,
            @Param("now") Instant now);

    /**
     * Tra lai: bo tiep nhan va DAT LAI dong ho failsafe (acknowledgedAt = null, tinh lai tu luc tra)
     * - tra lai nghia la khong con ai lo ca nay, phai de failsafe bao ve tiep.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("UPDATE TriageAlert a SET a.claimedById = null, a.claimedByType = null, a.claimedAt = null, "
            + "a.acknowledgedAt = null, a.needsInterventionAt = :now, a.updatedAt = :now "
            + "WHERE a.id = :id AND a.status = " + STATUS + "NEEDS_INTERVENTION "
            + "AND a.claimedById = :claimerId AND a.claimedByType = :claimerType")
    int releaseIfClaimedBy(
            @Param("id") Long id,
            @Param("claimerId") Long claimerId,
            @Param("claimerType") String claimerType,
            @Param("now") Instant now);

    /** ADMIN tra lai ho ca dang do nguoi khac tiep nhan. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("UPDATE TriageAlert a SET a.claimedById = null, a.claimedByType = null, a.claimedAt = null, "
            + "a.acknowledgedAt = null, a.needsInterventionAt = :now, a.updatedAt = :now "
            + "WHERE a.id = :id AND a.status = " + STATUS + "NEEDS_INTERVENTION AND a.claimedById IS NOT NULL")
    int releaseAsAdmin(
            @Param("id") Long id,
            @Param("now") Instant now);

    /**
     * Dong ca dang mo. Theo SCR-AD-13 "1 Admin bat ky" xu ly duoc, nen khong bat buoc la nguoi tiep
     * nhan; resolvedBy ghi dung nguoi dong (PATIENT khi benh nhan tu bam [An toan]).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("UPDATE TriageAlert a SET a.status = " + STATUS + "RESOLVED, "
            + "a.outcome = :outcome, a.resolutionNote = :note, "
            + "a.resolvedAt = :now, a.resolvedById = :resolverId, a.resolvedByType = :resolverType, "
            + "a.updatedAt = :now "
            + "WHERE a.id = :id AND a.status <> " + STATUS + "RESOLVED")
    int closeIfOpen(
            @Param("id") Long id,
            @Param("resolverId") Long resolverId,
            @Param("resolverType") String resolverType,
            @Param("outcome") TriageAlertOutcome outcome,
            @Param("note") String note,
            @Param("now") Instant now);
}
