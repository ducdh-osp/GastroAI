package vn.gastroai.be.application.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import vn.gastroai.be.domain.rag.Document;
import vn.gastroai.be.domain.rag.DocumentStatus;
import vn.gastroai.be.infrastructure.persistence.postgres.DocumentRepository;

import java.util.List;


@Component
public class DocumentRecoveryRunner {

    private static final Logger log = LoggerFactory.getLogger(DocumentRecoveryRunner.class);

    private final DocumentRepository documentRepository;
    private final DocumentProcessingWorker documentProcessingWorker;

    public DocumentRecoveryRunner(
            DocumentRepository documentRepository,
            DocumentProcessingWorker documentProcessingWorker) {
        this.documentRepository = documentRepository;
        this.documentProcessingWorker = documentProcessingWorker;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverUnfinishedDocuments() {
        List<Document> stuckDocuments = documentRepository.findByStatusIn(
                List.of(DocumentStatus.PENDING, DocumentStatus.PROCESSING));

        if (stuckDocuments.isEmpty()) {
            return;
        }

        log.info("Phat hien {} document con dang do o PENDING/PROCESSING sau khi server khoi dong lai - xu ly lai tu dau",
                stuckDocuments.size());

        for (Document document : stuckDocuments) {
            documentProcessingWorker.processAsync(document.getId(), document.getSource());
        }
    }
}