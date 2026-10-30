package vn.gastroai.be.domain.notification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "email_template_revisions")
@Getter
@NoArgsConstructor
public class EmailTemplateRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_code", nullable = false, length = 60)
    private EmailTemplateCode templateCode;

    @Column(name = "version_number", nullable = false)
    private long versionNumber;

    @Column(name = "subject_template", nullable = false, length = 255)
    private String subjectTemplate;

    @Column(name = "html_body_template", nullable = false, columnDefinition = "TEXT")
    private String htmlBodyTemplate;

    @Column(name = "text_body_template", nullable = false, columnDefinition = "TEXT")
    private String textBodyTemplate;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by_type", length = 20)
    private String updatedByType;

    @Column(name = "updated_by_id")
    private Long updatedById;

    public EmailTemplateRevision(EmailTemplate template) {
        this.templateCode = template.getCode();
        this.versionNumber = template.getVersion();
        this.subjectTemplate = template.getSubjectTemplate();
        this.htmlBodyTemplate = template.getHtmlBodyTemplate();
        this.textBodyTemplate = template.getTextBodyTemplate();
        this.updatedAt = template.getUpdatedAt();
        this.updatedByType = template.getUpdatedByType();
        this.updatedById = template.getUpdatedById();
    }
}
