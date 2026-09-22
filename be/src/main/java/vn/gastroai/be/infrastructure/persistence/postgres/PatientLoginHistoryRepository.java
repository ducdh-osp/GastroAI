package vn.gastroai.be.infrastructure.persistence.postgres;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.auth.PatientLoginHistory;
import java.time.Instant;
public interface PatientLoginHistoryRepository extends JpaRepository<PatientLoginHistory,Long> {
 Page<PatientLoginHistory> findByPatientId(Long patientId, Pageable pageable);
 long deleteByAttemptedAtBefore(Instant cutoff);
}
