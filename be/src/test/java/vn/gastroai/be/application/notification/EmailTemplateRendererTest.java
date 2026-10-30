package vn.gastroai.be.application.notification;

import org.junit.jupiter.api.Test;
import vn.gastroai.be.domain.notification.EmailTemplateCode;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmailTemplateRendererTest {

    private final EmailTemplateRenderer renderer = new EmailTemplateRenderer();

    @Test
    void rendersTextAndEscapesVariablesInHtml() {
        EmailTemplateContent content = new EmailTemplateContent(
                "Xác thực cho {{userName}}",
                "<p>Chào {{userName}}</p><a href=\"{{verificationUrl}}\">Mở</a>",
                "Chào {{userName}}: {{verificationUrl}}");

        RenderedEmail result = renderer.render(EmailTemplateCode.ACCOUNT_VERIFICATION, content,
                Map.of("userName", "A <B>", "verificationUrl", "https://example.com?a=1&b=2"));

        assertEquals("Xác thực cho A <B>", result.subject());
        assertTrue(result.htmlBody().contains("A &lt;B&gt;"));
        assertTrue(result.htmlBody().contains("a=1&amp;b=2"));
        assertTrue(result.textBody().contains("A <B>"));
    }

    @Test
    void rejectsUnknownPlaceholder() {
        EmailTemplateContent content = new EmailTemplateContent(
                "Tiêu đề {{serverSecret}}",
                "<a href=\"{{verificationUrl}}\">Mở</a>",
                "{{verificationUrl}}");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> renderer.validate(EmailTemplateCode.ACCOUNT_VERIFICATION, content));

        assertTrue(error.getMessage().contains("serverSecret"));
    }

    @Test
    void requiresActionLinkInBothHtmlAndTextFallback() {
        EmailTemplateContent content = new EmailTemplateContent(
                "Xác thực", "<p>Không có link</p>", "{{verificationUrl}}");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> renderer.validate(EmailTemplateCode.ACCOUNT_VERIFICATION, content));

        assertTrue(error.getMessage().contains("verificationUrl"));
        assertTrue(error.getMessage().contains("HTML"));
    }

    @Test
    void rejectsExecutableHtmlAndHeaderInjection() {
        EmailTemplateContent script = new EmailTemplateContent(
                "Nhắc thuốc", "<script>alert(1)</script><p>{{applicationUrl}}</p>",
                "{{applicationUrl}}");
        assertThrows(IllegalArgumentException.class,
                () -> renderer.validate(EmailTemplateCode.MEDICATION_REMINDER_OFFLINE, script));

        EmailTemplateContent newline = new EmailTemplateContent(
                "Tiêu đề\nBcc: attacker@example.com", "<p>{{applicationUrl}}</p>",
                "{{applicationUrl}}");
        assertThrows(IllegalArgumentException.class,
                () -> renderer.validate(EmailTemplateCode.MEDICATION_REMINDER_OFFLINE, newline));
    }
}
