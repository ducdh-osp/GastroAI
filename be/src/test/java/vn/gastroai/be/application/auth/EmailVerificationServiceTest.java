package vn.gastroai.be.application.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailVerificationServiceTest {
    private final JavaMailSender sender = mock(JavaMailSender.class);
    private final EmailVerificationService service = new EmailVerificationService(sender,
            "no-reply@gastroai.vn", "http://localhost:5173/verify-email",
            "http://localhost:5173/reset-password");

    @Test
    void verificationMailContainsOneTimeTokenAndVerificationLink() {
        service.send("user@example.com", "verify-token");

        var message = forClass(SimpleMailMessage.class);
        verify(sender).send(message.capture());
        assertEquals("no-reply@gastroai.vn", message.getValue().getFrom());
        assertEquals("user@example.com", message.getValue().getTo()[0]);
        assertEquals("Xac thuc tai khoan GastroAI", message.getValue().getSubject());
        assertEquals("Xac thuc tai khoan: http://localhost:5173/verify-email?token=verify-token"
                + "\nLien ket co hieu luc 24 gio.", message.getValue().getText());
    }

    @Test
    void passwordResetMailUsesResetRouteAndThirtyMinuteTtl() {
        service.sendPasswordReset("user@example.com", "reset-token");

        var message = forClass(SimpleMailMessage.class);
        verify(sender).send(message.capture());
        assertEquals("Dat lai mat khau GastroAI", message.getValue().getSubject());
        assertEquals("Dat lai mat khau: http://localhost:5173/reset-password?token=reset-token"
                + "\nLien ket co hieu luc 30 phut.", message.getValue().getText());
    }
}
