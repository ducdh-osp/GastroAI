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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Patient p where p.email = :email")
    Optional<Patient> findByEmailForUpdate(@Param("email") String email);
}
