package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.patient.BristolLog;

import java.time.Instant;
import java.util.List;

public interface BristolLogRepository extends JpaRepository<BristolLog, Long> {

    Page<BristolLog> findByPatientIdOrderByLoggedAtDesc(Long patientId, Pageable pageable);

    List<BristolLog> findByPatientIdAndLoggedAtBetweenOrderByLoggedAtAsc(Long patientId, Instant from, Instant to);
}
