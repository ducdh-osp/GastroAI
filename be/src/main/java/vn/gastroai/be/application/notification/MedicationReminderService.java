package vn.gastroai.be.application.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.api.notification.MedicationConfirmationDetail;
import vn.gastroai.be.api.notification.MedicationConfirmationListResponse;
import vn.gastroai.be.api.notification.MedicationConfirmationResponse;
import vn.gastroai.be.api.notification.MedicationReminderRequest;
import vn.gastroai.be.api.notification.MedicationReminderResponse;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.notification.MedicationConfirmation;
import vn.gastroai.be.domain.notification.MedicationReminder;
import vn.gastroai.be.infrastructure.persistence.postgres.MedicationConfirmationRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.MedicationReminderRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;

import java.util.List;

/** UC0015 (lich nhac uong thuoc) + UC0016 (xac nhan da uong - GD4 chi la nut thu cong,
 * chua tich hop notification/scheduler that, viec do la GD7). */
@Service
public class MedicationReminderService {

    private final MedicationReminderRepository medicationReminderRepository;
    private final MedicationConfirmationRepository medicationConfirmationRepository;
    private final PatientRepository patientRepository;

    public MedicationReminderService(MedicationReminderRepository medicationReminderRepository,
                                      MedicationConfirmationRepository medicationConfirmationRepository,
                                      PatientRepository patientRepository) {
        this.medicationReminderRepository = medicationReminderRepository;
        this.medicationConfirmationRepository = medicationConfirmationRepository;
        this.patientRepository = patientRepository;
    }

    @Transactional("postgresTransactionManager")
    public MedicationReminderResponse create(Long patientId, MedicationReminderRequest request) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));
        MedicationReminder reminder = new MedicationReminder(
                patient, request.medicineName(), request.dosage(), request.timeOfDay(), request.active());
        return toResponse(medicationReminderRepository.save(reminder));
    }

    @Transactional("postgresTransactionManager")
    public MedicationReminderResponse update(Long patientId, Long reminderId, MedicationReminderRequest request) {
        MedicationReminder reminder = loadOwnedReminder(patientId, reminderId);
        reminder.setMedicineName(request.medicineName());
        reminder.setDosage(request.dosage());
        reminder.setTimeOfDay(request.timeOfDay());
        reminder.setActive(request.active());
        return toResponse(medicationReminderRepository.save(reminder));
    }

    @Transactional("postgresTransactionManager")
    public void delete(Long patientId, Long reminderId) {
        medicationReminderRepository.delete(loadOwnedReminder(patientId, reminderId));
    }

    // Khong phan trang - 1 benh nhan thuc te khong co hang tram lich nhac thuoc, giong
    // tien le ChatHistoryController.getSessionMessages() tra List tran o quy mo tuong tu.
    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public List<MedicationReminderResponse> list(Long patientId) {
        return medicationReminderRepository.findByPatientIdOrderByTimeOfDayAsc(patientId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional("postgresTransactionManager")
    public MedicationConfirmationResponse confirmDose(Long patientId, Long reminderId) {
        MedicationReminder reminder = loadOwnedReminder(patientId, reminderId);
        MedicationConfirmation saved = medicationConfirmationRepository.save(new MedicationConfirmation(reminder));
        return new MedicationConfirmationResponse(saved.getId(), reminder.getId(), saved.getConfirmedAt());
    }

    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public MedicationConfirmationListResponse listConfirmations(Long patientId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<MedicationConfirmation> result =
                medicationConfirmationRepository.findByReminder_Patient_IdOrderByConfirmedAtDesc(patientId, pageable);
        List<MedicationConfirmationDetail> items = result.getContent().stream()
                .map(c -> new MedicationConfirmationDetail(
                        c.getId(), c.getReminder().getId(), c.getReminder().getMedicineName(), c.getConfirmedAt()))
                .toList();
        return new MedicationConfirmationListResponse(items, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    private MedicationReminder loadOwnedReminder(Long patientId, Long reminderId) {
        return medicationReminderRepository.findById(reminderId)
                .filter(r -> r.getPatient().getId().equals(patientId))
                .orElseThrow(() -> new IllegalArgumentException("Medication reminder not found or access denied"));
    }

    private MedicationReminderResponse toResponse(MedicationReminder reminder) {
        return new MedicationReminderResponse(
                reminder.getId(), reminder.getMedicineName(), reminder.getDosage(),
                reminder.getTimeOfDay(), reminder.isActive());
    }
}
