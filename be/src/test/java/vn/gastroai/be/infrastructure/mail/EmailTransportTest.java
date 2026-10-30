package vn.gastroai.be.infrastructure.mail;

import jakarta.mail.Session;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import vn.gastroai.be.application.notification.RenderedEmail;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailTransportTest {

    @Test
    void createsUtf8MultipartAlternativeMessage() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        EmailTransport transport = new EmailTransport(sender, "no-reply@gastroai.vn");

        transport.send("user@example.com", new RenderedEmail(
                "Nhắc uống thuốc", "<p>Đã đến giờ uống thuốc</p>", "Đã đến giờ uống thuốc"));

        verify(sender).send(message);
        assertEquals("Nhắc uống thuốc", message.getSubject());
        assertEquals("user@example.com", message.getAllRecipients()[0].toString());
        assertInstanceOf(Multipart.class, message.getContent());
    }
}
