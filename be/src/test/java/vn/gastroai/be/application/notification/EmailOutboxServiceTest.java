package vn.gastroai.be.application.notification;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.notification.*;
import vn.gastroai.be.infrastructure.persistence.postgres.EmailOutboxRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmailOutboxServiceTest {

    private final EmailOutboxRepository repository = mock(EmailOutboxRepository.class);
    private final EmailPayloadCipher cipher = mock(EmailPayloadCipher.class);
    private final EmailOutboxService service = new EmailOutboxService(
            repository, cipher,
            "https://app.example/verify-email", "https://app.example/reset-password",
            "https://app.example/", 3);

    @Test
    void queuesVerificationWithEncryptedVariablesAndCancelsOlderPendingJob() {
        Patient patient = patient();
        patient.setVerificationTokenHash("hash");
        when(cipher.encrypt(anyMap())).thenReturn("encrypted");
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EmailOutbox result = service.enqueueAccountVerification(patient, "raw token");

        verify(repository).cancelPendingForPatientAndTemplate(eq(7L),
                eq(EmailTemplateCode.ACCOUNT_VERIFICATION), eq(EmailOutboxStatus.PENDING),
                eq(EmailOutboxStatus.CANCELLED), any(Instant.class));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> variables = ArgumentCaptor.forClass(Map.class);
        verify(cipher).encrypt(variables.capture());
        assertEquals("https://app.example/verify-email?token=raw token",
                variables.getValue().get("verificationUrl"));
        assertEquals("encrypted", result.getEncryptedPayload());
        assertEquals("hash", result.getExpectedTokenHash());
        assertEquals(7L, result.getPatientId());
    }

    @Test
    void medicationEmailIsGenericDelayedAndDeduplicated() {
        Patient patient = patient();
        MedicationReminder reminder = new MedicationReminder(
                patient, "Sensitive medicine", "Sensitive dosage",
                java.util.List.of(LocalTime.of(8, 0)), null, null, null, true);
        reminder.setId(12L);
        when(cipher.encrypt(anyMap())).thenReturn("encrypted");
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EmailOutbox queued = service.enqueueMedicationReminder(
                reminder, LocalDate.of(2026, 10, 29), LocalTime.of(8, 0));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> variables = ArgumentCaptor.forClass(Map.class);
        verify(cipher).encrypt(variables.capture());
        assertEquals(Map.of("applicationUrl", "https://app.example/medications"), variables.getValue());
        assertFalse(variables.getValue().toString().contains("Sensitive"));
        assertEquals(Instant.parse("2026-10-29T01:03:00Z"), queued.getNextAttemptAt());
        assertEquals("MEDICATION_EMAIL:12:2026-10-29:08:00", queued.getDedupeKey());

        when(repository.existsByDedupeKey(queued.getDedupeKey())).thenReturn(true);
        assertNull(service.enqueueMedicationReminder(
                reminder, LocalDate.of(2026, 10, 29), LocalTime.of(8, 0)));
        verify(repository, times(1)).save(any());
    }

    private Patient patient() {
        Patient patient = new Patient();
        patient.setId(7L);
        patient.setEmail("user@example.com");
        patient.setFullName("Nguyễn Văn A");
        return patient;
    }
}
