package vn.gastroai.be.infrastructure.persistence.postgres;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.gastroai.be.domain.auth.Patient;

import java.time.Instant;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    @Query("select p.id from Patient p where p.emailVerified = true")
    java.util.List<Long> findAllVerifiedIds();

    @Query(value = """
            SELECT DISTINCT p.id
            FROM patients p
            WHERE p.email_verified = true
              AND (
                EXISTS (
                    SELECT 1 FROM chat_sessions cs
                    JOIN chat_messages cm ON cm.session_id = cs.id
                    WHERE cs.patient_id = p.id AND cm.created_at >= :since
                )
                OR EXISTS (
                    SELECT 1 FROM symptom_assessments sa
                    WHERE sa.patient_id = p.id AND sa.assessed_at >= :since
                )
              )
            """, nativeQuery = true)
    java.util.List<Long> findVerifiedIdsActiveSince(@Param("since") Instant since);

    @Query(value = """
            SELECT DISTINCT p.id
            FROM patients p
            JOIN triage_alerts ta ON ta.patient_id = p.id
            WHERE p.email_verified = true
              AND (ta.occurred_at >= :since OR ta.status <> 'RESOLVED')
            """, nativeQuery = true)
    java.util.List<Long> findVerifiedIdsWithRecentTriage(@Param("since") Instant since);

    @Query(value = """
            SELECT count(DISTINCT patient_id) FROM (
                SELECT cs.patient_id AS patient_id
                FROM chat_sessions cs
                JOIN chat_messages cm ON cm.session_id = cs.id
                WHERE cm.created_at >= :startDate AND cm.created_at < :endDate
                UNION
                SELECT sa.patient_id AS patient_id
                FROM symptom_assessments sa
                WHERE sa.assessed_at >= :startDate AND sa.assessed_at < :endDate
            ) active_patients
            """, nativeQuery = true)
    long countActivePatientsBetween(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    Optional<Patient> findByEmail(String email);

    Optional<Patient> findByVerificationTokenHash(String hash);

    Optional<Patient> findByPasswordResetTokenHash(String hash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from Patient p
            where p.emailVerified = false
              and p.verificationTokenExpiresAt is not null
              and p.verificationTokenExpiresAt <= :now
            """)
    long deleteExpiredUnverified(@Param("now") Instant now);

    @Query("""
            select p from Patient p
            where (:q = ''
                    or lower(p.email) like lower(concat('%', :q, '%')) escape '!'
                    or lower(coalesce(p.fullName, '')) like lower(concat('%', :q, '%')) escape '!')
              and (:adminLocked is null
                   or (:adminLocked = true and p.adminLockedAt is not null)
                   or (:adminLocked = false and p.adminLockedAt is null))
            """)
    Page<Patient> searchForAdmin(
            @Param("q") String q,
            @Param("adminLocked") Boolean adminLocked,
            Pageable pageable);

    /**
     * Giống findByEmail nhưng khoá row (SELECT ... FOR UPDATE) — dùng riêng cho AuthService.login()
     * để 2 request đăng nhập sai cùng lúc của cùng 1 tài khoản không bị race condition khi
     * cùng đọc/tăng failedLoginAttempts (request sau phải đợi request trước commit xong).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Patient p where p.email = :email")
    Optional<Patient> findByEmailForUpdate(@Param("email") String email);

    /** Khoá row theo id khi đổi mật khẩu, tránh các lần thử đồng thời làm mất lượt sai. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Patient p where p.id = :id")
    Optional<Patient> findByIdForUpdate(@Param("id") Long id);
}
