package vn.gastroai.be.application.notification;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import vn.gastroai.be.domain.notification.EmailTemplate;
import vn.gastroai.be.domain.notification.EmailTemplateCode;
import vn.gastroai.be.domain.notification.EmailTemplateRevision;
import vn.gastroai.be.infrastructure.persistence.postgres.EmailTemplateRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.EmailTemplateRevisionRepository;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailTemplateServiceTest {

    private final EmailTemplateRepository repository = mock(EmailTemplateRepository.class);
    private final EmailTemplateRevisionRepository revisions = mock(EmailTemplateRevisionRepository.class);
    private final EmailTemplateRenderer renderer = new EmailTemplateRenderer();
    private final EmailTemplateService service = new EmailTemplateService(repository, revisions, renderer);

    @Test
    void updatesWithOptimisticVersionAndStoresRevision() {
        EmailTemplate template = template();
        when(repository.findById(EmailTemplateCode.ACCOUNT_VERIFICATION)).thenReturn(Optional.of(template));
        when(repository.saveAndFlush(template)).thenAnswer(invocation -> {
            template.setVersion(3L);
            template.setUpdatedAt(Instant.parse("2026-10-29T10:00:00Z"));
            return template;
        });
        EmailTemplateContent content = new EmailTemplateContent(
                "Tiêu đề mới", "<p>{{verificationUrl}}</p>", "{{verificationUrl}}");

        EmailTemplateView result = service.update(
                EmailTemplateCode.ACCOUNT_VERIFICATION, content, 2L, "ADMIN", 9L);

        assertEquals("Tiêu đề mới", result.subjectTemplate());
        assertEquals(3L, result.version());
        assertEquals("ADMIN", result.updatedByType());
        assertEquals(9L, result.updatedById());
        ArgumentCaptor<EmailTemplateRevision> revision = ArgumentCaptor.forClass(EmailTemplateRevision.class);
        verify(revisions).save(revision.capture());
        assertEquals(3L, revision.getValue().getVersionNumber());
    }

    @Test
    void rejectsStaleVersionWithoutSaving() {
        EmailTemplate template = template();
        when(repository.findById(EmailTemplateCode.ACCOUNT_VERIFICATION)).thenReturn(Optional.of(template));
        EmailTemplateContent content = new EmailTemplateContent(
                "Tiêu đề", "<p>{{verificationUrl}}</p>", "{{verificationUrl}}");

        assertThrows(IllegalStateException.class, () -> service.update(
                EmailTemplateCode.ACCOUNT_VERIFICATION, content, 1L, "ADMIN", 9L));

        verify(repository, never()).saveAndFlush(any());
        verifyNoInteractions(revisions);
    }

    @Test
    void rejectsUnsafeHtmlBeforeSaving() {
        EmailTemplateContent content = new EmailTemplateContent(
                "Tiêu đề",
                "<a href=\"javascript&#58;alert(1)\">Mở</a><p>{{verificationUrl}}</p>",
                "{{verificationUrl}}");

        assertThrows(IllegalArgumentException.class, () -> service.update(
                EmailTemplateCode.ACCOUNT_VERIFICATION, content, 2L, "ADMIN", 9L));

        verifyNoInteractions(repository, revisions);
    }

    @Test
    void previewUsesSafeSampleVariables() {
        RenderedEmail preview = service.preview(
                EmailTemplateCode.MEDICATION_REMINDER_OFFLINE,
                new EmailTemplateContent("Nhắc thuốc", "<a href=\"{{applicationUrl}}\">Mở</a>",
                        "Mở {{applicationUrl}}"));

        assertTrue(preview.htmlBody().contains("https://gastroai.vn/medications"));
        assertTrue(preview.textBody().contains("https://gastroai.vn/medications"));
    }

    private EmailTemplate template() {
        EmailTemplate template = new EmailTemplate();
        template.setCode(EmailTemplateCode.ACCOUNT_VERIFICATION);
        template.setDisplayName("Xác thực tài khoản");
        template.setSubjectTemplate("Cũ");
        template.setHtmlBodyTemplate("<p>{{verificationUrl}}</p>");
        template.setTextBodyTemplate("{{verificationUrl}}");
        template.setVersion(2L);
        template.setUpdatedAt(Instant.parse("2026-10-28T10:00:00Z"));
        return template;
    }
}
