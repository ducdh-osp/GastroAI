package vn.gastroai.be.application.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.notification.EmailOutbox;
import vn.gastroai.be.domain.notification.EmailOutboxStatus;
import vn.gastroai.be.domain.notification.EmailTemplateCode;
import vn.gastroai.be.domain.notification.MedicationReminder;
import vn.gastroai.be.infrastructure.persistence.postgres.EmailOutboxRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

@Service
public class EmailOutboxService {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final EmailOutboxRepository repository;
    private final EmailPayloadCipher cipher;
    private final String verificationBaseUrl;
    private final String resetPasswordBaseUrl;
    private final String applicationBaseUrl;
    private final Duration medicationGrace;

    public EmailOutboxService(
            EmailOutboxRepository repository,
            EmailPayloadCipher cipher,
            @Value("${app.mail.verification-base-url}") String verificationBaseUrl,
            @Value("${app.mail.reset-password-base-url}") String resetPasswordBaseUrl,
            @Value("${app.mail.application-base-url}") String applicationBaseUrl,
            @Value("${app.mail.medication-offline-grace-minutes:3}") long medicationGraceMinutes) {
        this.repository = repository;
        this.cipher = cipher;
        this.verificationBaseUrl = stripTrailingSlash(verificationBaseUrl);
        this.resetPasswordBaseUrl = stripTrailingSlash(resetPasswordBaseUrl);
        this.applicationBaseUrl = stripTrailingSlash(applicationBaseUrl);
        this.medicationGrace = Duration.ofMinutes(Math.max(0, medicationGraceMinutes));
    }

    @Transactional("postgresTransactionManager")
    public EmailOutbox enqueueAccountVerification(Patient patient, String rawToken) {
        Instant now = Instant.now();
        cancelPending(patient.getId(), EmailTemplateCode.ACCOUNT_VERIFICATION, now);
        Map<String, String> variables = Map.of(
                "userName", displayName(patient),
                "verificationUrl", verificationBaseUrl + "?token=" + rawToken);
        EmailOutbox outbox = new EmailOutbox(
                EmailTemplateCode.ACCOUNT_VERIFICATION,
                patient.getEmail(), cipher.encrypt(variables),
                "ACCOUNT_VERIFICATION:" + patient.getId() + ":" + patient.getVerificationTokenHash(), now);
        outbox.setPatientId(patient.getId());
        outbox.setExpectedTokenHash(patient.getVerificationTokenHash());
        return repository.save(outbox);
    }

    @Transactional("postgresTransactionManager")
    public EmailOutbox enqueuePasswordReset(Patient patient, String rawToken) {
        Instant now = Instant.now();
        cancelPending(patient.getId(), EmailTemplateCode.PASSWORD_RESET, now);
        Map<String, String> variables = Map.of(
                "userName", displayName(patient),
                "resetUrl", resetPasswordBaseUrl + "?token=" + rawToken);
        EmailOutbox outbox = new EmailOutbox(
                EmailTemplateCode.PASSWORD_RESET,
                patient.getEmail(), cipher.encrypt(variables),
                "PASSWORD_RESET:" + patient.getId() + ":" + patient.getPasswordResetTokenHash(), now);
        outbox.setPatientId(patient.getId());
        outbox.setExpectedTokenHash(patient.getPasswordResetTokenHash());
        return repository.save(outbox);
    }

    @Transactional("postgresTransactionManager")
    public EmailOutbox enqueueMedicationReminder(MedicationReminder reminder, LocalDate date,
                                                  LocalTime scheduledTime) {
        String timeKey = scheduledTime.withSecond(0).withNano(0).toString();
        String dedupeKey = "MEDICATION_EMAIL:" + reminder.getId() + ":" + date + ":" + timeKey;
        if (repository.existsByDedupeKey(dedupeKey)) {
            return null;
        }
        Instant sendAfter = date.atTime(scheduledTime).atZone(VIETNAM_ZONE).toInstant().plus(medicationGrace);
        Map<String, String> variables = Map.of("applicationUrl", applicationBaseUrl + "/medications");
        EmailOutbox outbox = new EmailOutbox(
                EmailTemplateCode.MEDICATION_REMINDER_OFFLINE,
                reminder.getPatient().getEmail(), cipher.encrypt(variables), dedupeKey, sendAfter);
        outbox.setPatientId(reminder.getPatient().getId());
        outbox.setMedicationReminderId(reminder.getId());
        outbox.setScheduledDate(date);
        outbox.setScheduledTime(scheduledTime);
        return repository.save(outbox);
    }

    @Transactional("postgresTransactionManager")
    public EmailOutbox enqueueTest(EmailTemplateCode code, String recipientEmail) {
        EmailOutbox outbox = new EmailOutbox(
                code, recipientEmail.trim().toLowerCase(), cipher.encrypt(code.sampleVariables()),
                "EMAIL_TEMPLATE_TEST:" + UUID.randomUUID(), Instant.now());
        return repository.save(outbox);
    }

    @Transactional("postgresTransactionManager")
    public void cancelPending(Long patientId, EmailTemplateCode code) {
        cancelPending(patientId, code, Instant.now());
    }

    private void cancelPending(Long patientId, EmailTemplateCode code, Instant now) {
        if (patientId == null) {
            return;
        }
        repository.cancelPendingForPatientAndTemplate(patientId, code,
                EmailOutboxStatus.PENDING, EmailOutboxStatus.CANCELLED, now);
    }

    private String displayName(Patient patient) {
        return patient.getFullName() == null || patient.getFullName().isBlank()
                ? "bạn" : patient.getFullName().trim();
    }

    private static String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("URL cấu hình email không được để trống");
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
