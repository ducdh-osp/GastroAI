package vn.gastroai.be.application.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import vn.gastroai.be.infrastructure.persistence.postgres.DocumentRepository;

@Component
public class DocumentRecoveryRunner {

    private static final Logger log = LoggerFactory.getLogger(DocumentRecoveryRunner.class);

    private final DocumentRepository documentRepository;

    public DocumentRecoveryRunner(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void requeueStuckDocuments() {
        int requeuedCount = documentRepository.requeueStuckProcessing();

        if (requeuedCount > 0) {
            log.info("Da doi {} document tu PROCESSING ve PENDING sau khi server khoi dong lai - "
                    + "DocumentProcessingScheduler se tu nhat lai", requeuedCount);
        }
    }
}