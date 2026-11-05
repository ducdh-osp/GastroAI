package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.triage.TriageAlertAction;

import java.util.List;

public interface TriageAlertActionRepository extends JpaRepository<TriageAlertAction, Long> {
    List<TriageAlertAction> findByAlertIdOrderByCreatedAtAscIdAsc(Long alertId);
}
