package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.rag.Document;

public interface DocumentRepository extends JpaRepository<Document, Long> {
}