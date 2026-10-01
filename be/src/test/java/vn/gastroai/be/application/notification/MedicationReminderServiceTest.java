package vn.gastroai.be.application.notification;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import vn.gastroai.be.api.notification.MedicationConfirmationResponse;
import vn.gastroai.be.api.notification.MedicationReminderRequest;
import vn.gastroai.be.api.notification.MedicationReminderResponse;
import vn.gastroai.be.api.notification.MedicationConfirmationRequest;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.notification.MedicationConfirmation;
import vn.gastroai.be.domain.notification.MedicationReminder;
import vn.gastroai.be.infrastructure.persistence.postgres.MedicationConfirmationRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.MedicationReminderRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;

import java.time.LocalTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MedicationReminderServiceTest {

    private final MedicationReminderRepository medicationReminderRepository = mock(MedicationReminderRepository.class);
    private final MedicationConfirmationRepository medicationConfirmationRepository = mock(MedicationConfirmationRepository.class);
    private final PatientRepository patientRepository = mock(PatientRepository.class);
    private final MedicationReminderService service = new MedicationReminderService(
            medicationReminderRepository, medicationConfirmationRepository, patientRepository);

    @Test
    void confirmDoseThrowsWhenReminderBelongsToAnotherPatient() {
        Patient otherPatient = new Patient();
        otherPatient.setId(2L);
        MedicationReminder reminder = new MedicationReminder(otherPatient, "Omeprazole", "20mg", LocalTime.of(8, 0), true);
        when(medicationReminderRepository.findById(99L)).thenReturn(Optional.of(reminder));

        assertThrows(IllegalArgumentException.class, () -> service.confirmDose(
                1L, 99L, new MedicationConfirmationRequest(LocalTime.of(8, 0))));
    }

    @Test
    void confirmDoseSavesConfirmationLinkedToReminder() {
        Patient patient = new Patient();
        patient.setId(1L);
        MedicationReminder reminder = new MedicationReminder(patient, "Omeprazole", "20mg", LocalTime.of(8, 0), true);
        reminder.setId(5L);
        when(medicationReminderRepository.findById(5L)).thenReturn(Optional.of(reminder));
        when(medicationConfirmationRepository.saveAndFlush(any(MedicationConfirmation.class))).thenAnswer(inv -> {
            MedicationConfirmation saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        MedicationConfirmationResponse response = service.confirmDose(
                1L, 5L, new MedicationConfirmationRequest(LocalTime.of(8, 0)));

        assertEquals(5L, response.reminderId());
    }

    @Test
    void confirmDoseRejectsSecondConfirmationForSameReminderAndDay() {
        Patient patient = new Patient();
        patient.setId(1L);
        MedicationReminder reminder = new MedicationReminder(patient, "Omeprazole", "20mg", LocalTime.of(8, 0), true);
        reminder.setId(5L);
        when(medicationReminderRepository.findById(5L)).thenReturn(Optional.of(reminder));
        when(medicationConfirmationRepository.existsByReminder_IdAndConfirmationDateAndScheduledTime(
                org.mockito.ArgumentMatchers.eq(5L), any(LocalDate.class),
                org.mockito.ArgumentMatchers.eq(LocalTime.of(8, 0)))).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.confirmDose(
                1L, 5L, new MedicationConfirmationRequest(LocalTime.of(8, 0))));

        verify(medicationConfirmationRepository, never()).saveAndFlush(any());
    }

    @Test
    void deleteDeactivatesReminderWithoutDeletingItsHistory() {
        Patient patient = new Patient();
        patient.setId(1L);
        MedicationReminder reminder = new MedicationReminder(patient, "Omeprazole", "20mg", LocalTime.of(8, 0), true);
        reminder.setId(5L);
        when(medicationReminderRepository.findById(5L)).thenReturn(Optional.of(reminder));

        service.delete(1L, 5L);

        assertEquals(false, reminder.isActive());
        verify(medicationReminderRepository).save(reminder);
        verify(medicationReminderRepository, never()).delete(any());
    }

    @Test
    void confirmDoseReportsConflictWhenConcurrentRequestHitsUniqueConstraint() {
        Patient patient = new Patient();
        patient.setId(1L);
        MedicationReminder reminder = new MedicationReminder(patient, "Omeprazole", "20mg", LocalTime.of(8, 0), true);
        reminder.setId(5L);
        when(medicationReminderRepository.findById(5L)).thenReturn(Optional.of(reminder));
        when(medicationConfirmationRepository.saveAndFlush(any(MedicationConfirmation.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate reminder/date"));

        assertThrows(IllegalStateException.class, () -> service.confirmDose(
                1L, 5L, new MedicationConfirmationRequest(LocalTime.of(8, 0))));
    }

    @Test
    void updateCanToggleActiveOff() {
        Patient patient = new Patient();
        patient.setId(1L);
        MedicationReminder reminder = new MedicationReminder(patient, "Omeprazole", "20mg", LocalTime.of(8, 0), true);
        when(medicationReminderRepository.findById(5L)).thenReturn(Optional.of(reminder));
        when(medicationReminderRepository.save(any(MedicationReminder.class))).thenAnswer(inv -> inv.getArgument(0));

        MedicationReminderResponse response = service.update(1L, 5L,
                new MedicationReminderRequest("Omeprazole", "20mg", LocalTime.of(8, 0), false));

        assertEquals(false, response.active());
    }

    @Test
    void createSupportsMultipleDailyTimesAndTreatmentInstructions() {
        Patient patient = new Patient();
        patient.setId(1L);
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(medicationReminderRepository.save(any(MedicationReminder.class))).thenAnswer(inv -> {
            MedicationReminder saved = inv.getArgument(0);
            saved.setId(8L);
            return saved;
        });

        MedicationReminderResponse response = service.create(1L, new MedicationReminderRequest(
                "Amoxicillin", "1g", List.of(LocalTime.of(20, 0), LocalTime.of(8, 0)),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 14),
                "Uong sau an", true));

        assertEquals(List.of(LocalTime.of(8, 0), LocalTime.of(20, 0)), response.timesOfDay());
        assertEquals("Uong sau an", response.instructions());
    }

    @Test
    void confirmDoseRejectsTimeOutsideReminderSchedule() {
        Patient patient = new Patient();
        patient.setId(1L);
        MedicationReminder reminder = new MedicationReminder(patient, "Omeprazole", "20mg",
                List.of(LocalTime.of(8, 0)), null, null, "Truoc an 30 phut", true);
        reminder.setId(5L);
        when(medicationReminderRepository.findById(5L)).thenReturn(Optional.of(reminder));

        assertThrows(IllegalArgumentException.class, () -> service.confirmDose(
                1L, 5L, new MedicationConfirmationRequest(LocalTime.of(20, 0))));
    }
}
