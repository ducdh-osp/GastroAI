package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.gastroai.be.domain.triage.SymptomAssessmentRecord;
import vn.gastroai.be.infrastructure.persistence.postgres.projection.SymptomStatProjection;

import java.time.Instant;
import java.util.List;

public interface SymptomAssessmentRecordRepository extends JpaRepository<SymptomAssessmentRecord, Long> {
    List<SymptomAssessmentRecord> findTop100ByPatientIdOrderByAssessedAtDesc(Long patientId);

    long countByEmergencyTrue();

    @Query(value = """
            SELECT primary_symptom AS symptom,
                   count(*) AS totalCount,
                   count(*) FILTER (WHERE emergency = true) AS emergencyCount
            FROM symptom_assessments
            WHERE assessed_at >= :startDate AND assessed_at <= :endDate
            GROUP BY primary_symptom
            ORDER BY totalCount DESC
            """, nativeQuery = true)
    List<SymptomStatProjection> findSymptomStats(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);
}
