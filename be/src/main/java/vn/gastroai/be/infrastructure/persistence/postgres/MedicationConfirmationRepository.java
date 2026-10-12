package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.gastroai.be.domain.notification.MedicationConfirmation;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface MedicationConfirmationRepository extends JpaRepository<MedicationConfirmation, Long> {

    boolean existsByReminder_IdAndConfirmationDateAndScheduledTime(
            Long reminderId, LocalDate confirmationDate, LocalTime scheduledTime);

    @Query("select c from MedicationConfirmation c join fetch c.reminder r "
            + "where c.reminder.patient.id = :patientId and c.confirmationDate = :confirmationDate")
    List<MedicationConfirmation> findByPatientIdAndConfirmationDate(
            @Param("patientId") Long patientId, @Param("confirmationDate") LocalDate confirmationDate);

    // JOIN FETCH reminder - service.listConfirmations() doc reminder.getMedicineName() cho
    // moi dong, reminder la LAZY nen khong fetch san se gay N+1 query (1 SELECT/dong).
    @Query("select c from MedicationConfirmation c join fetch c.reminder r where r.patient.id = :patientId "
            + "order by c.confirmedAt desc")
    Page<MedicationConfirmation> findByReminder_Patient_IdOrderByConfirmedAtDesc(
            @Param("patientId") Long patientId, Pageable pageable);
}
