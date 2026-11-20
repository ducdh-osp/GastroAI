package vn.gastroai.be.application.document;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import vn.gastroai.be.domain.rag.Chunk;
import vn.gastroai.be.domain.rag.Document;
import vn.gastroai.be.infrastructure.ai.GeminiEmbeddingClient;
import vn.gastroai.be.infrastructure.persistence.postgres.DocumentRepository;
import vn.gastroai.be.infrastructure.rag.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Path;
import java.util.List;

@Service
public class DocumentProcessingWorker {

    // UC0033 - ten model dung khi luu embedding, khop voi model GeminiEmbeddingClient dang goi.
    private static final String EMBEDDING_MODEL = "gemini-embedding-2";
    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingWorker.class);
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

    /** Bước đang chạy, để báo lỗi theo đúng bước cho người dùng CMS. */
    enum Step { EXTRACTION, CHUNKING, EMBEDDING }

    @Async("documentProcessingExecutor")
    public void processAsync(Long documentId, String source) {

        Step step = Step.EXTRACTION;
        try {
            if (documentRepository.findById(documentId).isEmpty()) {
                log.warn("Document id={} da bi xoa truoc khi xu ly bat dau - bo qua", documentId);
                return;
            }

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

            step = Step.CHUNKING;
            documentStatusService.markChunking(documentId);
            List<Chunk> chunks = documentChunkingService.chunkDocument(documentId);

            step = Step.EMBEDDING;
            documentStatusService.markEmbedding(documentId);
            for (Chunk chunk : chunks) {
                float[] vector = embeddingClient.embed(chunk.getContent());
                embeddingStore.save(chunk.getId(), vector, EMBEDDING_MODEL);
            }

            documentStatusService.markDone(documentId);

        } catch (Exception e) {
            log.error("Loi xu ly document id={}", documentId, e);
            documentStatusService.markError(documentId, userMessage(step, e));
        }
    }

    /**
     * SCR-AD-04 §3.1 - lý do lỗi phải là câu người dùng hiểu được, không phải mã lỗi kỹ thuật.
     * Lỗi đọc file đã có câu tiếng Việt riêng nên giữ nguyên; lỗi khác đổi theo bước đang chạy,
     * còn chi tiết kỹ thuật chỉ ghi vào log ở trên.
     */
    static String userMessage(Step step, Exception e) {
        if (e instanceof ExtractionException && e.getMessage() != null && !e.getMessage().isBlank()) {
            return e.getMessage();
        }
        return switch (step) {
            case EXTRACTION -> "Không đọc được nội dung file. Hãy kiểm tra file mở được bình thường rồi tải lên lại.";
            case CHUNKING -> "Không chia được nội dung tài liệu thành các đoạn. Bấm \"Thử lại\"; nếu vẫn lỗi, báo nhóm kỹ thuật.";
            case EMBEDDING -> "Dịch vụ AI tạm thời không phản hồi khi tạo dữ liệu tra cứu. Bấm \"Thử lại\" sau ít phút.";
        };
    }
}