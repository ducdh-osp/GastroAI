package vn.gastroai.be.api.cms.email;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.gastroai.be.application.audit.AuditLogService;
import vn.gastroai.be.application.notification.EmailOutboxService;
import vn.gastroai.be.application.notification.EmailTemplateContent;
import vn.gastroai.be.application.notification.EmailTemplateService;
import vn.gastroai.be.application.notification.EmailTemplateView;
import vn.gastroai.be.application.notification.RenderedEmail;
import vn.gastroai.be.domain.audit.AuditAction;
import vn.gastroai.be.domain.notification.EmailTemplateCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** UC0074 - chi Admin duoc xem/sua/preview/gui thu mau email he thong. */
@RestController
@RequestMapping("/api/v1/cms/email-templates")
public class EmailTemplateAdminController {

    private static final String TARGET = "EMAIL_TEMPLATE";

    private final EmailTemplateService templateService;
    private final EmailOutboxService outboxService;
    private final AuditLogService auditLogService;

    public EmailTemplateAdminController(EmailTemplateService templateService,
                                        EmailOutboxService outboxService,
                                        AuditLogService auditLogService) {
        this.templateService = templateService;
        this.outboxService = outboxService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public List<EmailTemplateView> list() {
        return templateService.list();
    }

    @GetMapping("/{code}")
    public EmailTemplateView get(@PathVariable EmailTemplateCode code) {
        return templateService.get(code);
    }

    @PutMapping("/{code}")
    public EmailTemplateView update(@PathVariable EmailTemplateCode code,
                                    @Valid @RequestBody UpdateEmailTemplateRequest request,
                                    Authentication authentication) {
        EmailTemplateView before = templateService.get(code);
        EmailTemplateView after = templateService.update(code, content(request), request.version(),
                "ADMIN", Long.valueOf(authentication.getName()));
        auditLogService.record(AuditAction.EMAIL_TEMPLATE_UPDATE, TARGET, code.name(), describeUpdate(before, after));
        return after;
    }

    @PostMapping("/preview")
    public RenderedEmail preview(@Valid @RequestBody PreviewEmailTemplateRequest request) {
        return templateService.preview(request.code(), new EmailTemplateContent(
                request.subjectTemplate(), request.htmlBodyTemplate(), request.textBodyTemplate()));
    }

    @PostMapping("/{code}/test")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> sendTest(@PathVariable EmailTemplateCode code,
                                        @Valid @RequestBody TestEmailTemplateRequest request) {
        outboxService.enqueueTest(code, request.recipientEmail());
        auditLogService.record(AuditAction.EMAIL_TEMPLATE_TEST_SEND, TARGET, code.name(),
                "Gửi thử tới: " + request.recipientEmail());
        return Map.of("message", "Email thử đã được đưa vào hàng đợi gửi");
    }

    static String describeUpdate(EmailTemplateView before, EmailTemplateView after) {
        List<String> changes = new ArrayList<>();
        if (!before.subjectTemplate().equals(after.subjectTemplate())) {
            changes.add("Tiêu đề: «" + before.subjectTemplate() + "» → «" + after.subjectTemplate() + "»");
        }
        if (!before.htmlBodyTemplate().equals(after.htmlBodyTemplate())) {
            changes.add("Nội dung HTML: " + before.htmlBodyTemplate().length() + " → "
                    + after.htmlBodyTemplate().length() + " ký tự");
        }
        if (!before.textBodyTemplate().equals(after.textBodyTemplate())) {
            changes.add("Nội dung text: " + before.textBodyTemplate().length() + " → "
                    + after.textBodyTemplate().length() + " ký tự");
        }
        String summary = changes.isEmpty() ? "Lưu mẫu, không thay đổi nội dung" : String.join("; ", changes);
        return summary + " · phiên bản " + before.version() + " → " + after.version();
    }

    private EmailTemplateContent content(UpdateEmailTemplateRequest request) {
        return new EmailTemplateContent(request.subjectTemplate(),
                request.htmlBodyTemplate(), request.textBodyTemplate());
    }
}