package vn.gastroai.be.application.auth;

import org.junit.jupiter.api.Test;
import vn.gastroai.be.application.notification.EmailOutboxService;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.notification.EmailTemplateCode;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailVerificationServiceTest {
    private final EmailOutboxService outbox = mock(EmailOutboxService.class);
    private final EmailVerificationService service = new EmailVerificationService(outbox);

    @Test
    void verificationMailIsQueuedWithPatientAndOneTimeToken() {
        Patient patient = new Patient();
        patient.setId(7L);
        patient.setEmail("user@example.com");

        service.send(patient, "verify-token");

        verify(outbox).enqueueAccountVerification(patient, "verify-token");
    }

    @Test
    void passwordResetMailIsQueuedWithPatientAndOneTimeToken() {
        Patient patient = new Patient();
        patient.setId(7L);
        patient.setEmail("user@example.com");

        service.sendPasswordReset(patient, "reset-token");

        verify(outbox).enqueuePasswordReset(patient, "reset-token");
    }

    @Test
    void cancellationDelegatesToOutbox() {
        service.cancelVerification(7L);
        service.cancelPasswordReset(7L);

        verify(outbox).cancelPending(7L, EmailTemplateCode.ACCOUNT_VERIFICATION);
        verify(outbox).cancelPending(7L, EmailTemplateCode.PASSWORD_RESET);
    }
}
