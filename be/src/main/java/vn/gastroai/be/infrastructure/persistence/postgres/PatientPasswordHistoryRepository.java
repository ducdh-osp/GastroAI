package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.gastroai.be.domain.auth.PatientPasswordHistory;

import java.util.List;

public interface PatientPasswordHistoryRepository extends JpaRepository<PatientPasswordHistory, Long> {

    @Query("select history.passwordHash from PatientPasswordHistory history where history.patient.id = :patientId")
    List<String> findPasswordHashesByPatientId(@Param("patientId") Long patientId);
}
