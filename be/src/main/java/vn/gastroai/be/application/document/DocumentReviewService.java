package vn.gastroai.be.application.document;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.api.document.DocumentReviewResponse;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.domain.rag.Document;
import vn.gastroai.be.infrastructure.filestorage.FileStorageService;
import vn.gastroai.be.infrastructure.persistence.postgres.ChunkRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.DocumentRepository;

import java.nio.file.Path;
import java.util.Locale;

/**
 * SCR-AD-04 §3.1b - cho Biên tập đối chiếu file gốc với các đoạn chatbot sẽ trích dẫn,
 * trước khi bấm "Xác nhận dùng cho AI".
 */
@Service
public class DocumentReviewService {

    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final FileStorageService fileStorageService;

    public DocumentReviewService(DocumentRepository documentRepository, ChunkRepository chunkRepository,
                                 FileStorageService fileStorageService) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.fileStorageService = fileStorageService;
    }

    /** File gốc của 1 phiên bản. */
    public record OriginalFile(Path path, String fileType) {
    }

    @Transactional(readOnly = true)
    public DocumentReviewResponse review(Long documentId) {
        Document document = find(documentId);
        String fileType = fileStorageService.findStored(document.getSource())
                .map(DocumentReviewService::fileTypeOf)
                .orElse(null);

            return new DocumentReviewResponse(
                document.getId(),
                document.getTitle(),
                document.getStatus(),
                document.isInUse(),
                document.getContentApprovedAt() != null,
                document.getContentApprovedBy(),
                document.getContentApprovedAt(),
                fileType,
                // SCR-AD-04 §3.2: xac nhan ban thay the se XOA VINH VIEN ban dang dung - FE canh bao truoc.
                document.isReplacement(),
                "PDF".equals(fileType) ? null : document.getFullText(),
                chunkRepository.findByDocumentIdOrderByChunkIndex(documentId).stream()
                        .map(chunk -> new DocumentReviewResponse.Chunk(chunk.getChunkIndex(), chunk.getContent()))
                        .toList());
    }

    @Transactional(readOnly = true)
    public OriginalFile originalFile(Long documentId) {
        Document document = find(documentId);
        return fileStorageService.findStored(document.getSource())
                .map(path -> new OriginalFile(path, fileTypeOf(path)))
                .orElseThrow(() -> new ResourceNotFoundException("Tài liệu này không còn file gốc để xem"));
    }

    private Document find(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu: " + documentId));
    }

    static String fileTypeOf(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".pdf")) {
            return "PDF";
        }
        if (name.endsWith(".docx")) {
            return "DOCX";
        }
        return null;
    }
}