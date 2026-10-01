package vn.gastroai.be.application.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.application.support.OwnedResourceLoader;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.api.notification.MedicationConfirmationDetail;
import vn.gastroai.be.api.notification.MedicationConfirmationListResponse;
import vn.gastroai.be.api.notification.MedicationConfirmationRequest;
import vn.gastroai.be.api.notification.MedicationConfirmationResponse;
import vn.gastroai.be.api.notification.MedicationReminderRequest;
import vn.gastroai.be.api.notification.MedicationReminderResponse;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.notification.MedicationConfirmation;
import vn.gastroai.be.domain.notification.MedicationReminder;
import vn.gastroai.be.infrastructure.persistence.postgres.MedicationConfirmationRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.MedicationReminderRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

/** UC0015 (lich nhac uong thuoc) + UC0016 (xac nhan da uong - GD4 chi la nut thu cong,
 * chua tich hop notification/scheduler that, viec do la GD7). */
@Service
public class MedicationReminderService {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

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
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay benh nhan"));
        validateRequest(request);
        MedicationReminder reminder = new MedicationReminder(
                patient, request.medicineName(), request.dosage(), normalizedTimes(request),
                request.startDate(), request.endDate(), request.instructions(), request.active());
        return toResponse(medicationReminderRepository.save(reminder), Set.of());
    }

    @Transactional("postgresTransactionManager")
    public MedicationReminderResponse update(Long patientId, Long reminderId, MedicationReminderRequest request) {
        MedicationReminder reminder = loadOwnedReminder(patientId, reminderId);
        validateRequest(request);
        reminder.setMedicineName(request.medicineName());
        reminder.setDosage(request.dosage());
        reminder.setTimesOfDay(new ArrayList<>(normalizedTimes(request)));
        reminder.setStartDate(request.startDate());
        reminder.setEndDate(request.endDate());
        reminder.setInstructions(request.instructions());
        reminder.setActive(request.active());
        LocalDate today = Instant.now().atZone(VN_ZONE).toLocalDate();
        Set<LocalTime> confirmedTimes = medicationConfirmationRepository
                .findByPatientIdAndConfirmationDate(patientId, today).stream()
                .filter(confirmation -> confirmation.getReminder().getId().equals(reminderId))
                .map(MedicationConfirmation::getScheduledTime)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        return toResponse(medicationReminderRepository.save(reminder), confirmedTimes);
    }

    @Transactional("postgresTransactionManager")
    public void delete(Long patientId, Long reminderId) {
        // A DELETE request means stop future reminders. Keep the row so its confirmation
        // history remains intact and can still be shown to the patient.
        MedicationReminder reminder = loadOwnedReminder(patientId, reminderId);
        reminder.setActive(false);
        medicationReminderRepository.save(reminder);
    }

    // Khong phan trang - 1 benh nhan thuc te khong co hang tram lich nhac thuoc, giong
    // tien le ChatHistoryController.getSessionMessages() tra List tran o quy mo tuong tu.
    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public List<MedicationReminderResponse> list(Long patientId) {
        LocalDate today = Instant.now().atZone(VN_ZONE).toLocalDate();
        Map<Long, Set<LocalTime>> confirmedByReminder = medicationConfirmationRepository
                .findByPatientIdAndConfirmationDate(patientId, today).stream()
                .filter(confirmation -> confirmation.getScheduledTime() != null)
                .collect(Collectors.groupingBy(
                        confirmation -> confirmation.getReminder().getId(),
                        Collectors.mapping(MedicationConfirmation::getScheduledTime, Collectors.toSet())));
        return medicationReminderRepository.findByPatientIdOrderByCreatedAtAsc(patientId).stream()
                .map(reminder -> toResponse(reminder,
                        confirmedByReminder.getOrDefault(reminder.getId(), Set.of())))
                .toList();
    }

    @Transactional("postgresTransactionManager")
    public MedicationConfirmationResponse confirmDose(
            Long patientId, Long reminderId, MedicationConfirmationRequest request) {
        MedicationReminder reminder = loadOwnedReminder(patientId, reminderId);
        Instant confirmedAt = Instant.now();
        LocalDate confirmationDate = confirmedAt.atZone(VN_ZONE).toLocalDate();
        if (!reminder.isActive()
                || (reminder.getStartDate() != null && confirmationDate.isBefore(reminder.getStartDate()))
                || (reminder.getEndDate() != null && confirmationDate.isAfter(reminder.getEndDate()))) {
            throw new IllegalStateException("Lich thuoc khong hoat dong trong ngay hom nay.");
        }
        if (!reminder.getTimesOfDay().contains(request.scheduledTime())) {
            throw new IllegalArgumentException("Gio uong khong thuoc lich nhac nay.");
        }
        if (medicationConfirmationRepository.existsByReminder_IdAndConfirmationDateAndScheduledTime(
                reminderId, confirmationDate, request.scheduledTime())) {
            throw new IllegalStateException("Lieu thuoc nay da duoc xac nhan trong hom nay.");
        }

        // The DB unique index is the final guard if two requests pass the check together.
        try {
            MedicationConfirmation saved = medicationConfirmationRepository.saveAndFlush(
                    new MedicationConfirmation(reminder, confirmedAt, confirmationDate, request.scheduledTime()));
            return new MedicationConfirmationResponse(
                    saved.getId(), reminder.getId(), saved.getScheduledTime(), saved.getConfirmedAt());
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalStateException("Lieu thuoc nay da duoc xac nhan trong hom nay.", exception);
        }
    }

    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public MedicationConfirmationListResponse listConfirmations(Long patientId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<MedicationConfirmation> result =
                medicationConfirmationRepository.findByReminder_Patient_IdOrderByConfirmedAtDesc(patientId, pageable);
        List<MedicationConfirmationDetail> items = result.getContent().stream()
                .map(c -> new MedicationConfirmationDetail(
                        c.getId(), c.getReminder().getId(), c.getReminder().getMedicineName(),
                        c.getScheduledTime(), c.getConfirmedAt()))
                .toList();
        return new MedicationConfirmationListResponse(items, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    private MedicationReminder loadOwnedReminder(Long patientId, Long reminderId) {
        return OwnedResourceLoader.loadOwned(medicationReminderRepository.findById(reminderId),
                r -> r.getPatient().getId().equals(patientId),
                "Khong tim thay lich nhac thuoc hoac ban khong co quyen truy cap");
    }

    private MedicationReminderResponse toResponse(
            MedicationReminder reminder, Set<LocalTime> confirmedTimesToday) {
        return new MedicationReminderResponse(
                reminder.getId(), reminder.getMedicineName(), reminder.getDosage(),
                reminder.getTimesOfDay(), reminder.getStartDate(), reminder.getEndDate(),
                reminder.getInstructions(), reminder.isActive(), confirmedTimesToday);
    }

    private void validateRequest(MedicationReminderRequest request) {
        if (request.startDate() != null && request.endDate() != null
                && request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("Ngay ket thuc khong duoc truoc ngay bat dau.");
        }
        if (new HashSet<>(request.timesOfDay()).size() != request.timesOfDay().size()) {
            throw new IllegalArgumentException("Cac gio nhac khong duoc trung nhau.");
        }
    }

    private List<LocalTime> normalizedTimes(MedicationReminderRequest request) {
        return request.timesOfDay().stream().sorted(Comparator.naturalOrder()).toList();
    }
}
