package vn.gastroai.be.infrastructure.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import vn.gastroai.be.application.notification.RenderedEmail;

@Component
public class EmailTransport {

    private final JavaMailSender mailSender;
    private final String from;

    public EmailTransport(JavaMailSender mailSender, @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void send(String recipient, RenderedEmail email) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(recipient);
            helper.setSubject(email.subject());
            helper.setText(email.textBody(), email.htmlBody());
            mailSender.send(message);
        } catch (MessagingException exception) {
            throw new MailSendException("Không thể tạo email MIME", exception);
        }
    }
}
