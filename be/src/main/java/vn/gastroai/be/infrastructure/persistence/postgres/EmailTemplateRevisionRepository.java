package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.notification.EmailTemplateRevision;

public interface EmailTemplateRevisionRepository extends JpaRepository<EmailTemplateRevision, Long> {
}
