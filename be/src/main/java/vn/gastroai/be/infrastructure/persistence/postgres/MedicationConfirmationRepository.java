package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.notification.MedicationConfirmation;

public interface MedicationConfirmationRepository extends JpaRepository<MedicationConfirmation, Long> {

    // Dieu huong qua 2 cap association (reminder -> patient -> id) - dau "_" de tach ro
    // ranh gioi property, tranh Spring Data hieu nham thanh 1 field ten "ReminderPatientId".
    Page<MedicationConfirmation> findByReminder_Patient_IdOrderByConfirmedAtDesc(Long patientId, Pageable pageable);
}
