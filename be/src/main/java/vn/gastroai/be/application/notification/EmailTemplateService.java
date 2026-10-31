package vn.gastroai.be.application.notification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.domain.notification.EmailTemplate;
import vn.gastroai.be.domain.notification.EmailTemplateCode;
import vn.gastroai.be.domain.notification.EmailTemplateRevision;
import vn.gastroai.be.infrastructure.persistence.postgres.EmailTemplateRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.EmailTemplateRevisionRepository;

import java.util.Comparator;
import java.util.List;

@Service
public class EmailTemplateService {

    private final EmailTemplateRepository repository;
    private final EmailTemplateRevisionRepository revisionRepository;
    private final EmailTemplateRenderer renderer;

    public EmailTemplateService(EmailTemplateRepository repository,
                                EmailTemplateRevisionRepository revisionRepository,
                                EmailTemplateRenderer renderer) {
        this.repository = repository;
        this.revisionRepository = revisionRepository;
        this.renderer = renderer;
    }

    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public List<EmailTemplateView> list() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(template -> template.getCode().ordinal()))
                .map(this::toView)
                .toList();
    }

    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public EmailTemplateView get(EmailTemplateCode code) {
        return toView(find(code));
    }

    @Transactional("postgresTransactionManager")
    public EmailTemplateView update(EmailTemplateCode code, EmailTemplateContent content,
                                    long expectedVersion, String updatedByType, Long updatedById) {
        renderer.validate(code, content);
        EmailTemplate template = find(code);
        if (template.getVersion() != expectedVersion) {
            throw new IllegalStateException(
                    "Mẫu email đã được cập nhật ở nơi khác. Vui lòng tải lại trước khi lưu.");
        }
        template.setSubjectTemplate(content.subjectTemplate().trim());
        template.setHtmlBodyTemplate(content.htmlBodyTemplate().trim());
        template.setTextBodyTemplate(content.textBodyTemplate().trim());
        template.setUpdatedByType(updatedByType);
        template.setUpdatedById(updatedById);
        EmailTemplate saved = repository.saveAndFlush(template);
        revisionRepository.save(new EmailTemplateRevision(saved));
        return toView(saved);
    }

    public RenderedEmail render(EmailTemplateCode code, java.util.Map<String, String> variables) {
        EmailTemplate template = find(code);
        return renderer.render(code, toContent(template), variables);
    }

    public RenderedEmail preview(EmailTemplateCode code, EmailTemplateContent content) {
        return renderer.render(code, content, code.sampleVariables());
    }

    private EmailTemplate find(EmailTemplateCode code) {
        return repository.findById(code)
                .orElseThrow(() -> new IllegalStateException("Thiếu mẫu email hệ thống: " + code));
    }

    private EmailTemplateContent toContent(EmailTemplate template) {
        return new EmailTemplateContent(template.getSubjectTemplate(),
                template.getHtmlBodyTemplate(), template.getTextBodyTemplate());
    }

    private EmailTemplateView toView(EmailTemplate template) {
        return new EmailTemplateView(
                template.getCode().name(), template.getDisplayName(),
                template.getSubjectTemplate(), template.getHtmlBodyTemplate(),
                template.getTextBodyTemplate(), template.getCode().sortedAllowedVariables(),
                template.getVersion(), template.getUpdatedAt(), template.getUpdatedByType(),
                template.getUpdatedById());
    }
}
