package vn.gastroai.be.application.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.notification.EmailOutbox;
import vn.gastroai.be.domain.notification.EmailOutboxStatus;
import vn.gastroai.be.domain.notification.MedicationReminder;
import vn.gastroai.be.infrastructure.mail.EmailTransport;
import vn.gastroai.be.infrastructure.persistence.postgres.EmailOutboxRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.MedicationConfirmationRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.MedicationReminderRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.security.PatientWebSocketSessionRegistry;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class EmailDeliveryService {

    private static final List<Duration> RETRY_DELAYS = List.of(
            Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(30));

    private final EmailOutboxRepository outboxRepository;
    private final PatientRepository patientRepository;
    private final MedicationReminderRepository reminderRepository;
    private final MedicationConfirmationRepository confirmationRepository;
    private final PatientWebSocketSessionRegistry presence;
    private final EmailPayloadCipher cipher;
    private final EmailTemplateService templateService;
    private final EmailTransport transport;
    private final int maxAttempts;

    public EmailDeliveryService(
            EmailOutboxRepository outboxRepository,
            PatientRepository patientRepository,
            MedicationReminderRepository reminderRepository,
            MedicationConfirmationRepository confirmationRepository,
            PatientWebSocketSessionRegistry presence,
            EmailPayloadCipher cipher,
            EmailTemplateService templateService,
            EmailTransport transport,
            @Value("${app.mail.max-attempts:4}") int maxAttempts) {
        this.outboxRepository = outboxRepository;
        this.patientRepository = patientRepository;
        this.reminderRepository = reminderRepository;
        this.confirmationRepository = confirmationRepository;
        this.presence = presence;
        this.cipher = cipher;
        this.templateService = templateService;
        this.transport = transport;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    /** Xu ly toi da mot email voi row lock; false nghia la khong con email den han. */
    @Transactional("postgresTransactionManager")
    public boolean processNext(Instant now) {
        Optional<EmailOutbox> found = outboxRepository
                .findFirstByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAscIdAsc(
                        EmailOutboxStatus.PENDING, now);
        if (found.isEmpty()) {
            return false;
        }
        EmailOutbox outbox = found.get();
        if (!eligible(outbox, now)) {
            outbox.setStatus(EmailOutboxStatus.SKIPPED);
            outbox.setLastError(null);
            return true;
        }

        try {
            Map<String, String> variables = cipher.decrypt(outbox.getEncryptedPayload());
            RenderedEmail rendered = templateService.render(outbox.getTemplateCode(), variables);
            transport.send(outbox.getRecipientEmail(), rendered);
            outbox.setStatus(EmailOutboxStatus.SENT);
            outbox.setSentAt(now);
            outbox.setLastError(null);
        } catch (RuntimeException exception) {
            recordFailure(outbox, now, exception);
        }
        return true;
    }

    private boolean eligible(EmailOutbox outbox, Instant now) {
        if (outbox.getDedupeKey().startsWith("EMAIL_TEMPLATE_TEST:")) {
            return true;
        }
        return switch (outbox.getTemplateCode()) {
            case ACCOUNT_VERIFICATION -> patientRepository.findById(outbox.getPatientId())
                    .filter(patient -> !patient.isEmailVerified())
                    .filter(patient -> tokenMatches(outbox, patient.getVerificationTokenHash()))
                    .filter(patient -> patient.getVerificationTokenExpiresAt() != null
                            && patient.getVerificationTokenExpiresAt().isAfter(now))
                    .isPresent();
            case PASSWORD_RESET -> patientRepository.findById(outbox.getPatientId())
                    .filter(patient -> tokenMatches(outbox, patient.getPasswordResetTokenHash()))
                    .filter(patient -> patient.getPasswordResetTokenExpiresAt() != null
                            && patient.getPasswordResetTokenExpiresAt().isAfter(now))
                    .isPresent();
            case MEDICATION_REMINDER_OFFLINE -> medicationEligible(outbox);
        };
    }

    private boolean tokenMatches(EmailOutbox outbox, String currentHash) {
        return outbox.getExpectedTokenHash() != null
                && outbox.getExpectedTokenHash().equals(currentHash);
    }

    private boolean medicationEligible(EmailOutbox outbox) {
        if (outbox.getPatientId() == null || outbox.getMedicationReminderId() == null
                || outbox.getScheduledDate() == null || outbox.getScheduledTime() == null) {
            return false;
        }
        if (presence.isOnline(outbox.getPatientId())) {
            return false;
        }
        MedicationReminder reminder = reminderRepository.findById(outbox.getMedicationReminderId()).orElse(null);
        if (reminder == null || !reminder.isActive()
                || !outbox.getPatientId().equals(reminder.getPatient().getId())
                || !reminder.getTimesOfDay().contains(outbox.getScheduledTime())) {
            return false;
        }
        if (reminder.getStartDate() != null && outbox.getScheduledDate().isBefore(reminder.getStartDate())) {
            return false;
        }
        if (reminder.getEndDate() != null && outbox.getScheduledDate().isAfter(reminder.getEndDate())) {
            return false;
        }
        return !confirmationRepository.existsByReminder_IdAndConfirmationDateAndScheduledTime(
                reminder.getId(), outbox.getScheduledDate(), outbox.getScheduledTime());
    }

    private void recordFailure(EmailOutbox outbox, Instant now, RuntimeException exception) {
        int attempts = outbox.getAttemptCount() + 1;
        outbox.setAttemptCount(attempts);
        String message = exception.getClass().getSimpleName() + ": "
                + Optional.ofNullable(exception.getMessage()).orElse("không có thông tin");
        outbox.setLastError(message.substring(0, Math.min(1000, message.length())));
        if (attempts >= maxAttempts) {
            outbox.setStatus(EmailOutboxStatus.FAILED);
            return;
        }
        Duration delay = RETRY_DELAYS.get(Math.min(attempts - 1, RETRY_DELAYS.size() - 1));
        outbox.setNextAttemptAt(now.plus(delay));
    }
}
