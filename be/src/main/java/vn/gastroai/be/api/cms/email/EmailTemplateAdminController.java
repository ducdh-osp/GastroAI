package vn.gastroai.be.api.cms.email;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.gastroai.be.application.notification.EmailOutboxService;
import vn.gastroai.be.application.notification.EmailTemplateContent;
import vn.gastroai.be.application.notification.EmailTemplateService;
import vn.gastroai.be.application.notification.EmailTemplateView;
import vn.gastroai.be.application.notification.RenderedEmail;
import vn.gastroai.be.domain.notification.EmailTemplateCode;

import java.util.List;
import java.util.Map;

/** UC0074 - chi Admin duoc xem/sua/preview/gui thu mau email he thong. */
@RestController
@RequestMapping("/api/v1/cms/email-templates")
public class EmailTemplateAdminController {

    private final EmailTemplateService templateService;
    private final EmailOutboxService outboxService;

    public EmailTemplateAdminController(EmailTemplateService templateService,
                                        EmailOutboxService outboxService) {
        this.templateService = templateService;
        this.outboxService = outboxService;
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
        return templateService.update(code, content(request), request.version(),
                "ADMIN", Long.valueOf(authentication.getName()));
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
        return Map.of("message", "Email thử đã được đưa vào hàng đợi gửi");
    }

    private EmailTemplateContent content(UpdateEmailTemplateRequest request) {
        return new EmailTemplateContent(request.subjectTemplate(),
                request.htmlBodyTemplate(), request.textBodyTemplate());
    }
}
