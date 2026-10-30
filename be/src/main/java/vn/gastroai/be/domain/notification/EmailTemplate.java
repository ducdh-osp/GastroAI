package vn.gastroai.be.domain.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "email_templates")
@Getter
@Setter
@NoArgsConstructor
public class EmailTemplate {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 60)
    private EmailTemplateCode code;

    @Column(name = "display_name", nullable = false, length = 160)
    private String displayName;

    @Column(name = "subject_template", nullable = false, length = 255)
    private String subjectTemplate;

    @Column(name = "html_body_template", nullable = false, columnDefinition = "TEXT")
    private String htmlBodyTemplate;

    @Column(name = "text_body_template", nullable = false, columnDefinition = "TEXT")
    private String textBodyTemplate;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "updated_by_type", length = 20)
    private String updatedByType;

    @Column(name = "updated_by_id")
    private Long updatedById;

    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
