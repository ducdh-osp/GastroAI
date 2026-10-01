package vn.gastroai.be.application.document;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.domain.rag.Document;
import vn.gastroai.be.domain.rag.DocumentProcessingStage;
import vn.gastroai.be.domain.rag.DocumentStatus;
import vn.gastroai.be.infrastructure.persistence.postgres.ChunkRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.DocumentRepository;

import java.util.Optional;

@Service
public class DocumentStatusService {

    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;

    public DocumentStatusService(DocumentRepository documentRepository, ChunkRepository chunkRepository) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Document> findDocument(Long documentId) {
        return documentRepository.findById(documentId);
    }

    @Transactional
    public void markProcessing(Long documentId) {
        Document document = getDocument(documentId);

        document.setStatus(DocumentStatus.PROCESSING);
        document.setProcessingStage(DocumentProcessingStage.EXTRACTION);
        document.setErrorMessage(null);

        documentRepository.save(document);
    }

    @Transactional
    public void markChunking(Long documentId) {
        Document document = getDocument(documentId);
        document.setProcessingStage(DocumentProcessingStage.CHUNKING);
        documentRepository.save(document);
    }

    @Transactional
    public void markEmbedding(Long documentId) {
        Document document = getDocument(documentId);
        document.setProcessingStage(DocumentProcessingStage.EMBEDDING);
        documentRepository.save(document);
    }

    @Transactional
    public void markDone(Long documentId) {
        Document document = getDocument(documentId);

        document.setStatus(DocumentStatus.DONE);
        document.setProcessingStage(DocumentProcessingStage.DONE);
        document.setErrorMessage(null);

        documentRepository.save(document);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markError(Long documentId, String errorMessage) {
        Document document = getDocument(documentId);

        document.setStatus(DocumentStatus.ERROR);
        document.setProcessingStage(DocumentProcessingStage.ERROR);
        document.setErrorMessage(errorMessage);

        documentRepository.save(document);
        chunkRepository.deleteByDocumentId(documentId);
    }

    private Document getDocument(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy document: " + documentId
                ));
    }
}