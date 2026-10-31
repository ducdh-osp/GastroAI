package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.gastroai.be.domain.notification.MedicationReminder;

import java.time.LocalDate;
import java.util.List;

public interface MedicationReminderRepository extends JpaRepository<MedicationReminder, Long> {

    List<MedicationReminder> findByPatientIdOrderByCreatedAtAsc(Long patientId);

    @Query("""
            select distinct r from MedicationReminder r
            join fetch r.patient
            where r.active = true
              and (r.startDate is null or r.startDate <= :date)
              and (r.endDate is null or r.endDate >= :date)
            """)
    List<MedicationReminder> findActiveForDate(@Param("date") LocalDate date);

    @Query("""
            select distinct r from MedicationReminder r
            join fetch r.patient
            where r.patient.id = :patientId
              and r.active = true
              and (r.startDate is null or r.startDate <= :date)
              and (r.endDate is null or r.endDate >= :date)
            """)
    List<MedicationReminder> findActiveForDateAndPatientId(
            @Param("patientId") Long patientId,
            @Param("date") LocalDate date);
}
