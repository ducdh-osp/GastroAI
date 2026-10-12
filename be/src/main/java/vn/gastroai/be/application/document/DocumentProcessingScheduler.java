package vn.gastroai.be.application.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.gastroai.be.domain.rag.Document;
import vn.gastroai.be.domain.rag.DocumentStatus;
import vn.gastroai.be.infrastructure.persistence.postgres.DocumentRepository;

import java.util.List;

@Component
public class DocumentProcessingScheduler {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingScheduler.class);

    private final DocumentRepository documentRepository;
    private final DocumentProcessingWorker documentProcessingWorker;

    private boolean recovered = false;

    public DocumentProcessingScheduler(
            DocumentRepository documentRepository,
            DocumentProcessingWorker documentProcessingWorker) {
        this.documentRepository = documentRepository;
        this.documentProcessingWorker = documentProcessingWorker;
    }

    @Scheduled(fixedDelay = 5000)
    public void dispatchPendingDocuments() {

        if (!recovered) {
            int requeuedCount = documentRepository.requeueStuckProcessing();
            if (requeuedCount > 0) {
                log.info("Da doi {} document tu PROCESSING ve PENDING sau khi server khoi dong lai",
                        requeuedCount);
            }
            recovered = true;
        }

        List<Document> pendingDocuments = documentRepository.findByStatusIn(
                List.of(DocumentStatus.PENDING));

        for (Document document : pendingDocuments) {
            int claimedRows = documentRepository.claimForProcessing(document.getId());

            if (claimedRows == 0) {
                // Da bi mot luot quet khac gianh mat truoc - bo qua, khong xu ly 2 lan.
                continue;
            }

            try {
                documentProcessingWorker.processAsync(document.getId(), document.getSource());
            } catch (TaskRejectedException e) {
                // Pool xu ly dang day hang doi - tra lai PENDING de lan quet sau thu lai,
                // khong de document "ket" o PROCESSING ma khong co ai thuc su xu ly.
                documentRepository.revertToPending(document.getId());
                log.warn("Pool xu ly day hang doi, tra document id={} ve PENDING de thu lai sau",
                        document.getId());
            }
        }
    }
}