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

    @Test
    void rejectsEntityEncodedJavascriptUrl() {
        EmailTemplateContent encodedColon = new EmailTemplateContent(
                "Nhắc thuốc",
                "<a href=\"javascript&#58;alert(1)\">Mở</a><p>{{applicationUrl}}</p>",
                "{{applicationUrl}}");
        EmailTemplateContent encodedLetter = new EmailTemplateContent(
                "Nhắc thuốc",
                "<a href=\"&#106;avascript:alert(1)\">Mở</a><p>{{applicationUrl}}</p>",
                "{{applicationUrl}}");
        EmailTemplateContent encodedNewline = new EmailTemplateContent(
                "Nhắc thuốc",
                "<a href=\"java&#10;script:alert(1)\">Mở</a><p>{{applicationUrl}}</p>",
                "{{applicationUrl}}");

        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> renderer.validate(EmailTemplateCode.MEDICATION_REMINDER_OFFLINE, encodedColon)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> renderer.validate(EmailTemplateCode.MEDICATION_REMINDER_OFFLINE, encodedLetter)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> renderer.validate(EmailTemplateCode.MEDICATION_REMINDER_OFFLINE, encodedNewline)));
    }

    @Test
    void rejectsHtmlOutsideTheSafelist() {
        EmailTemplateContent metaRefresh = new EmailTemplateContent(
                "Nhắc thuốc",
                "<meta http-equiv=\"refresh\" content=\"0;url=https://attacker.example\">"
                        + "<p>{{applicationUrl}}</p>",
                "{{applicationUrl}}");
        EmailTemplateContent inlineStyle = new EmailTemplateContent(
                "Nhắc thuốc",
                "<style>body{display:none}</style><p>{{applicationUrl}}</p>",
                "{{applicationUrl}}");
        EmailTemplateContent dataImage = new EmailTemplateContent(
                "Nhắc thuốc",
                "<img src=\"data:image/svg+xml,&lt;svg onload=alert(1)&gt;\">"
                        + "<p>{{applicationUrl}}</p>",
                "{{applicationUrl}}");

        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> renderer.validate(EmailTemplateCode.MEDICATION_REMINDER_OFFLINE, metaRefresh)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> renderer.validate(EmailTemplateCode.MEDICATION_REMINDER_OFFLINE, inlineStyle)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> renderer.validate(EmailTemplateCode.MEDICATION_REMINDER_OFFLINE, dataImage)));
    }

    @Test
    void allowsExpectedEmailMarkupAndHttpsLinks() {
        EmailTemplateContent content = new EmailTemplateContent(
                "Xác thực tài khoản",
                "<h2>Xác thực</h2><table><tbody><tr><td><strong>GastroAI</strong></td>"
                        + "<td><a href=\"{{verificationUrl}}\">Mở</a></td></tr></tbody></table>",
                "Mở {{verificationUrl}}");

        assertDoesNotThrow(() -> renderer.validate(EmailTemplateCode.ACCOUNT_VERIFICATION, content));
    }
}
