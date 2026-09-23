package vn.gastroai.be.infrastructure.persistence.postgres;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.gastroai.be.domain.auth.Patient;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByEmail(String email);

    Optional<Patient> findByVerificationTokenHash(String hash);

    Optional<Patient> findByPasswordResetTokenHash(String hash);

    /**
     * Giống findByEmail nhưng khoá row (SELECT ... FOR UPDATE) — dùng riêng cho AuthService.login()
     * để 2 request đăng nhập sai cùng lúc của cùng 1 tài khoản không bị race condition khi
     * cùng đọc/tăng failedLoginAttempts (request sau phải đợi request trước commit xong).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Patient p where p.email = :email")
    Optional<Patient> findByEmailForUpdate(@Param("email") String email);
}
