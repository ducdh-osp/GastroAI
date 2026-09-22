package vn.gastroai.be.application.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailVerificationService {
    private final JavaMailSender mail;
    private final String from;
    private final String verificationBaseUrl;
    private final String resetBaseUrl;

    public EmailVerificationService(
            JavaMailSender mail,
            @Value("${app.mail.from}") String from,
            @Value("${app.mail.verification-base-url}") String verificationBaseUrl,
            @Value("${app.mail.reset-password-base-url:http://localhost:5173/reset-password}") String resetBaseUrl) {
        this.mail = mail;
        this.from = from;
        this.verificationBaseUrl = verificationBaseUrl;
        this.resetBaseUrl = resetBaseUrl;
    }

    public void send(String to, String token) {
        sendMessage(to, "Xac thuc tai khoan GastroAI",
                "Xac thuc tai khoan: " + verificationBaseUrl + "?token=" + token
                        + "\nLien ket co hieu luc 24 gio.");
    }

    public void sendPasswordReset(String to, String token) {
        sendMessage(to, "Dat lai mat khau GastroAI",
                "Dat lai mat khau: " + resetBaseUrl + "?token=" + token
                        + "\nLien ket co hieu luc 30 phut.");
    }

    private void sendMessage(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mail.send(message);
    }
}
