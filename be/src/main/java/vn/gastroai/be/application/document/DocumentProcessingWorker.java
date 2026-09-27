package vn.gastroai.be.application.document;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import vn.gastroai.be.domain.rag.Document;
import vn.gastroai.be.infrastructure.persistence.postgres.DocumentRepository;

import java.nio.file.Path;

@Service
public class DocumentProcessingWorker {

    private final DocumentStatusService documentStatusService;
    private final DocumentExtractionService documentExtractionService;
    private final DocumentRepository documentRepository;
    private final DocumentChunkingService documentChunkingService;
    public DocumentProcessingWorker(
            DocumentStatusService documentStatusService,
            DocumentExtractionService documentExtractionService,
            DocumentRepository documentRepository,
            DocumentChunkingService documentChunkingService) {
        this.documentStatusService = documentStatusService;
        this.documentExtractionService = documentExtractionService;
        this.documentRepository = documentRepository;
        this.documentChunkingService = documentChunkingService;
    }

    @Async
    public void processAsync(Long documentId, String source) {

        try {

            documentStatusService.markProcessing(documentId);

            Path filePath = Path.of(source);

            String fullText = documentExtractionService.extract(filePath);

            if (fullText.isBlank()) {
                throw new ExtractionException(
                        "Document không có nội dung");
            }

            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Không tìm thấy document: " + documentId));

            document.setFullText(fullText);
            documentRepository.save(document);
            documentChunkingService.chunkDocument(documentId);
            documentStatusService.markDone(documentId);

        } catch (Exception e) {

            documentStatusService.markError(
                    documentId,
                    getErrorMessage(e));
        }
    }

    private String getErrorMessage(Exception e) {

        if (e.getMessage() != null && !e.getMessage().isBlank()) {
            return e.getMessage();
        }

        return "Lỗi không xác định khi xử lý document";
    }
}