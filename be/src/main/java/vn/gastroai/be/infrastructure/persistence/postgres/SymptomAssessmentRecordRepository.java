package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.triage.SymptomAssessmentRecord;

import java.util.List;

public interface SymptomAssessmentRecordRepository extends JpaRepository<SymptomAssessmentRecord, Long> {
    List<SymptomAssessmentRecord> findTop100ByPatientIdOrderByAssessedAtDesc(Long patientId);
}
