package vn.gastroai.be.application.document;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import vn.gastroai.be.domain.rag.Chunk;
import vn.gastroai.be.domain.rag.Document;
import vn.gastroai.be.infrastructure.ai.GeminiEmbeddingClient;
import vn.gastroai.be.infrastructure.persistence.postgres.DocumentRepository;
import vn.gastroai.be.infrastructure.rag.EmbeddingStore;

import java.nio.file.Path;
import java.util.List;

@Service
public class DocumentProcessingWorker {

    // UC0033 - ten model dung khi luu embedding, khop voi model GeminiEmbeddingClient dang goi.
    private static final String EMBEDDING_MODEL = "gemini-embedding-2";

    private final DocumentStatusService documentStatusService;
    private final DocumentExtractionService documentExtractionService;
    private final DocumentRepository documentRepository;
    private final DocumentChunkingService documentChunkingService;
    private final GeminiEmbeddingClient embeddingClient;
    private final EmbeddingStore embeddingStore;

    public DocumentProcessingWorker(
            DocumentStatusService documentStatusService,
            DocumentExtractionService documentExtractionService,
            DocumentRepository documentRepository,
            DocumentChunkingService documentChunkingService,
            GeminiEmbeddingClient embeddingClient,
            EmbeddingStore embeddingStore) {
        this.documentStatusService = documentStatusService;
        this.documentExtractionService = documentExtractionService;
        this.documentRepository = documentRepository;
        this.documentChunkingService = documentChunkingService;
        this.embeddingClient = embeddingClient;
        this.embeddingStore = embeddingStore;
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

            documentStatusService.markChunking(documentId);
            List<Chunk> chunks = documentChunkingService.chunkDocument(documentId);

            documentStatusService.markEmbedding(documentId);
            for (Chunk chunk : chunks) {
                float[] vector = embeddingClient.embed(chunk.getContent());
                embeddingStore.save(chunk.getId(), vector, EMBEDDING_MODEL);
            }

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
