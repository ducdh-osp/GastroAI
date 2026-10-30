package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.notification.EmailTemplate;
import vn.gastroai.be.domain.notification.EmailTemplateCode;

public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, EmailTemplateCode> {
}
