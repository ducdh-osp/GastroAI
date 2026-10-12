package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.patient.MedicalProfile;

import java.util.Optional;

public interface MedicalProfileRepository extends JpaRepository<MedicalProfile, Long> {

    Optional<MedicalProfile> findByPatientId(Long patientId);
}
