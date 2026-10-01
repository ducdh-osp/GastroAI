package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.triage.TriageAlert;
import vn.gastroai.be.domain.triage.TriageAlertStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TriageAlertRepository extends JpaRepository<TriageAlert, Long> {
    List<TriageAlert> findTop50ByOrderByOccurredAtDesc();
    Optional<TriageAlert> findFirstByPatient_IdAndStatusNotAndOccurredAtAfterOrderByOccurredAtDesc(
            Long patientId, TriageAlertStatus excludedStatus, Instant after);
}