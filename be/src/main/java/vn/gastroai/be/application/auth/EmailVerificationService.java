package vn.gastroai.be.application.auth;

import org.springframework.stereotype.Service;
import vn.gastroai.be.application.notification.EmailOutboxService;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.notification.EmailTemplateCode;

/**
 * Facade cho nghiep vu auth. Email duoc ghi vao outbox trong cung transaction voi token,
 * sau do worker gui bat dong bo va retry neu SMTP tam thoi loi.
 */
@Service
public class EmailVerificationService {
    private final EmailOutboxService outboxService;

    public EmailVerificationService(EmailOutboxService outboxService) {
        this.outboxService = outboxService;
    }

    public void send(Patient patient, String token) {
        outboxService.enqueueAccountVerification(patient, token);
    }

    public void sendPasswordReset(Patient patient, String token) {
        outboxService.enqueuePasswordReset(patient, token);
    }

    public void cancelVerification(Long patientId) {
        outboxService.cancelPending(patientId, EmailTemplateCode.ACCOUNT_VERIFICATION);
    }

    public void cancelPasswordReset(Long patientId) {
        outboxService.cancelPending(patientId, EmailTemplateCode.PASSWORD_RESET);
    }
}
