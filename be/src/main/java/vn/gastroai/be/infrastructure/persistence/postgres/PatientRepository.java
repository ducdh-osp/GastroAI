package vn.gastroai.be.infrastructure.persistence.postgres;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.gastroai.be.domain.auth.Patient;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    @Query("select p.id from Patient p where p.emailVerified = true")
    java.util.List<Long> findAllVerifiedIds();

    Optional<Patient> findByEmail(String email);

    Optional<Patient> findByVerificationTokenHash(String hash);

    Optional<Patient> findByPasswordResetTokenHash(String hash);

    @Query("""
            select p from Patient p
            where (:q is null
                    or lower(p.email) like lower(concat('%', :q, '%')) escape '!'
                    or lower(coalesce(p.fullName, '')) like lower(concat('%', :q, '%')) escape '!')
              and (:adminLocked is null or p.adminLocked = :adminLocked)
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
