package vn.gastroai.be.application.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.notification.*;
import vn.gastroai.be.infrastructure.mail.EmailTransport;
import vn.gastroai.be.infrastructure.persistence.postgres.*;
import vn.gastroai.be.infrastructure.security.PatientWebSocketSessionRegistry;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmailDeliveryServiceTest {

    private final EmailOutboxRepository outboxes = mock(EmailOutboxRepository.class);
    private final PatientRepository patients = mock(PatientRepository.class);
    private final MedicationReminderRepository reminders = mock(MedicationReminderRepository.class);
    private final MedicationConfirmationRepository confirmations = mock(MedicationConfirmationRepository.class);
    private final PatientWebSocketSessionRegistry presence = mock(PatientWebSocketSessionRegistry.class);
    private final EmailPayloadCipher cipher = mock(EmailPayloadCipher.class);
    private final EmailTemplateService templates = mock(EmailTemplateService.class);
    private final EmailTransport transport = mock(EmailTransport.class);
    private final EmailDeliveryService service = new EmailDeliveryService(
            outboxes, patients, reminders, confirmations, presence, cipher, templates, transport, 4);
    private final Instant now = Instant.parse("2026-10-29T01:03:00Z");

    @BeforeEach
    void commonRendering() {
        when(cipher.decrypt(anyString())).thenReturn(Map.of("applicationUrl", "https://app/medications"));
        when(templates.render(any(), anyMap())).thenReturn(new RenderedEmail("Subject", "<p>Body</p>", "Body"));
    }

    @Test
    void sendsMedicationEmailOnlyWhenOfflineAndUnconfirmed() {
        EmailOutbox outbox = medicationOutbox();
        MedicationReminder reminder = reminder();
        when(outboxes.findFirstByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAscIdAsc(
                EmailOutboxStatus.PENDING, now)).thenReturn(Optional.of(outbox));
        when(reminders.findById(12L)).thenReturn(Optional.of(reminder));

        assertTrue(service.processNext(now));

        verify(transport).send(eq("user@example.com"), any(RenderedEmail.class));
        assertEquals(EmailOutboxStatus.SENT, outbox.getStatus());
        assertEquals(now, outbox.getSentAt());
    }

    @Test
    void skipsMedicationEmailWhenPatientIsOnline() {
        EmailOutbox outbox = medicationOutbox();
        when(outboxes.findFirstByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAscIdAsc(
                EmailOutboxStatus.PENDING, now)).thenReturn(Optional.of(outbox));
        when(presence.isOnline(7L)).thenReturn(true);

        service.processNext(now);

        assertEquals(EmailOutboxStatus.SKIPPED, outbox.getStatus());
        verifyNoInteractions(transport);
    }

    @Test
    void skipsMedicationEmailWhenDoseWasConfirmedDuringGracePeriod() {
        EmailOutbox outbox = medicationOutbox();
        when(outboxes.findFirstByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAscIdAsc(
                EmailOutboxStatus.PENDING, now)).thenReturn(Optional.of(outbox));
        when(reminders.findById(12L)).thenReturn(Optional.of(reminder()));
        when(confirmations.existsByReminder_IdAndConfirmationDateAndScheduledTime(
                12L, LocalDate.of(2026, 10, 29), LocalTime.of(8, 0))).thenReturn(true);

        service.processNext(now);

        assertEquals(EmailOutboxStatus.SKIPPED, outbox.getStatus());
        verifyNoInteractions(transport);
    }

    @Test
    void retriesTransientMailFailureWithBackoff() {
        EmailOutbox outbox = medicationOutbox();
        when(outboxes.findFirstByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAscIdAsc(
                EmailOutboxStatus.PENDING, now)).thenReturn(Optional.of(outbox));
        when(reminders.findById(12L)).thenReturn(Optional.of(reminder()));
        doThrow(new MailSendException("SMTP unavailable")).when(transport).send(anyString(), any());

        service.processNext(now);

        assertEquals(EmailOutboxStatus.PENDING, outbox.getStatus());
        assertEquals(1, outbox.getAttemptCount());
        assertEquals(now.plusSeconds(60), outbox.getNextAttemptAt());
        assertTrue(outbox.getLastError().contains("SMTP unavailable"));
    }

    @Test
    void marksEmailFailedAfterMaximumAttempts() {
        EmailOutbox outbox = medicationOutbox();
        when(outboxes.findFirstByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAscIdAsc(
                eq(EmailOutboxStatus.PENDING), any(Instant.class))).thenReturn(Optional.of(outbox));
        when(reminders.findById(12L)).thenReturn(Optional.of(reminder()));
        doThrow(new MailSendException("SMTP unavailable")).when(transport).send(anyString(), any());

        service.processNext(now);
        service.processNext(now.plusSeconds(60));
        service.processNext(now.plusSeconds(360));
        service.processNext(now.plusSeconds(2160));

        assertEquals(EmailOutboxStatus.FAILED, outbox.getStatus());
        assertEquals(4, outbox.getAttemptCount());
        verify(transport, times(4)).send(anyString(), any());
    }

    @Test
    void skipsMalformedMedicationEmailWithoutSending() {
        EmailOutbox outbox = medicationOutbox();
        outbox.setScheduledTime(null);
        when(outboxes.findFirstByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAscIdAsc(
                EmailOutboxStatus.PENDING, now)).thenReturn(Optional.of(outbox));

        service.processNext(now);

        assertEquals(EmailOutboxStatus.SKIPPED, outbox.getStatus());
        verifyNoInteractions(transport);
    }

    @Test
    void skipsStaleVerificationTokenInsteadOfSendingOldLink() {
        EmailOutbox outbox = new EmailOutbox(
                EmailTemplateCode.ACCOUNT_VERIFICATION, "user@example.com", "encrypted",
                "ACCOUNT_VERIFICATION:7:old", now);
        outbox.setPatientId(7L);
        outbox.setExpectedTokenHash("old");
        Patient patient = patient();
        patient.setVerificationTokenHash("new");
        patient.setVerificationTokenExpiresAt(now.plusSeconds(3600));
        when(outboxes.findFirstByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAscIdAsc(
                EmailOutboxStatus.PENDING, now)).thenReturn(Optional.of(outbox));
        when(patients.findById(7L)).thenReturn(Optional.of(patient));

        service.processNext(now);

        assertEquals(EmailOutboxStatus.SKIPPED, outbox.getStatus());
        verifyNoInteractions(transport);
    }

    private EmailOutbox medicationOutbox() {
        EmailOutbox outbox = new EmailOutbox(
                EmailTemplateCode.MEDICATION_REMINDER_OFFLINE, "user@example.com", "encrypted",
                "MEDICATION_EMAIL:12:2026-10-29:08:00", now);
        outbox.setPatientId(7L);
        outbox.setMedicationReminderId(12L);
        outbox.setScheduledDate(LocalDate.of(2026, 10, 29));
        outbox.setScheduledTime(LocalTime.of(8, 0));
        return outbox;
    }

    private MedicationReminder reminder() {
        MedicationReminder reminder = new MedicationReminder(
                patient(), "Medicine", "Dose", List.of(LocalTime.of(8, 0)),
                null, null, null, true);
        reminder.setId(12L);
        return reminder;
    }

    private Patient patient() {
        Patient patient = new Patient();
        patient.setId(7L);
        patient.setEmail("user@example.com");
        return patient;
    }
}
