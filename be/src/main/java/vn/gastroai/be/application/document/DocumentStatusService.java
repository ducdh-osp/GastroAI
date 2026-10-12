package vn.gastroai.be.application.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.domain.rag.Document;
import vn.gastroai.be.domain.rag.DocumentProcessingStage;
import vn.gastroai.be.domain.rag.DocumentStatus;
import vn.gastroai.be.infrastructure.persistence.postgres.ChunkRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.DocumentRepository;

import java.util.List;
import java.util.Optional;

@Service
public class DocumentStatusService {

    private static final Logger log = LoggerFactory.getLogger(DocumentStatusService.class);

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
        Optional<Document> maybeDocument = documentRepository.findById(documentId);

        if (maybeDocument.isEmpty()) {
            log.warn("Document id={} khong con ton tai (co the da bi xoa) - bo qua markError", documentId);
            return;
        }

        Document document = maybeDocument.get();

        document.setStatus(DocumentStatus.ERROR);
        document.setProcessingStage(DocumentProcessingStage.ERROR);
        document.setErrorMessage(errorMessage);

        documentRepository.save(document);
        chunkRepository.deleteByDocumentId(documentId);
    }

    @Transactional(readOnly = true)
    public vn.gastroai.be.api.document.DocumentPageResponse list(
            String query,
            DocumentStatus status,
            int page,
            int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page >= 0 va size trong khoang 1..100");
        }

        String normalizedQuery = query == null || query.isBlank()
                ? ""
                : vn.gastroai.be.application.support.VietnameseText.normalize(query.trim());

        List<Document> all = documentRepository.findAllByOrderByUploadedAtDesc();

        List<Document> filtered = all.stream()
                .filter(d -> status == null || d.getStatus() == status)
                .filter(d -> normalizedQuery.isEmpty()
                        || (d.getTitle() != null && vn.gastroai.be.application.support.VietnameseText.normalize(d.getTitle()).contains(normalizedQuery))
                        || (d.getSourceUrl() != null && vn.gastroai.be.application.support.VietnameseText.normalize(d.getSourceUrl()).contains(normalizedQuery)))
                .toList();

        // Lấy số lượng chunks cho từng document
        java.util.Map<Long, Long> chunkCounts = chunkRepository.countByDocumentIdGrouped().stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        long offset = pageable.getOffset();
        if (offset >= filtered.size()) {
            return new vn.gastroai.be.api.document.DocumentPageResponse(List.of(), filtered.size(), page, size);
        }

        int start = Math.toIntExact(offset);
        int end = Math.min(start + size, filtered.size());

        List<vn.gastroai.be.api.document.DocumentSummaryResponse> items = filtered.subList(start, end).stream()
                .map(d -> new vn.gastroai.be.api.document.DocumentSummaryResponse(
                        d.getId(),
                        d.getTitle(),
                        d.getSource(),
                        d.getSourceUrl(),
                        d.getStatus(),
                        d.getProcessingStage(),
                        d.getErrorMessage(),
                        chunkCounts.getOrDefault(d.getId(), 0L),
                        d.getUploadedAt(),
                        d.getUpdatedAt()
                ))
                .toList();

        return new vn.gastroai.be.api.document.DocumentPageResponse(items, filtered.size(), page, size);
    }

    @Transactional
    public Document retry(Long documentId) {
        Document document = getDocument(documentId);
        if (document.getStatus() != DocumentStatus.ERROR) {
            throw new IllegalStateException("Chỉ có thể thử lại các tài liệu đang ở trạng thái lỗi (ERROR)");
        }
        document.setStatus(DocumentStatus.PENDING);
        document.setProcessingStage(DocumentProcessingStage.PENDING);
        document.setErrorMessage(null);
        return documentRepository.save(document);
    }

    private Document getDocument(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy document: " + documentId
                ));
    }
}