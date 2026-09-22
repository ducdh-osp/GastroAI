package vn.gastroai.be.infrastructure.persistence.postgres;
import org.springframework.data.jpa.repository.JpaRepository; import vn.gastroai.be.domain.auth.Patient; import java.util.*;
public interface PatientRepository extends JpaRepository<Patient,Long>{ Optional<Patient> findByEmail(String email); Optional<Patient> findByVerificationTokenHash(String hash); }
