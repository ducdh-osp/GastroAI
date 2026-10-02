package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.rag.Document;
import vn.gastroai.be.domain.rag.DocumentStatus;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    /** Tim cac document dang do o nhung trang thai chua hoan tat - dung de phuc hoi sau khi server khoi dong lai. */
    List<Document> findByStatusIn(List<DocumentStatus> statuses);

    /** Tim document khac co cung ma bam noi dung, bo qua ban da ERROR (duoc phep upload lai). */
    Optional<Document> findFirstByContentHashAndStatusNot(String contentHash, DocumentStatus status);
}