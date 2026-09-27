package vn.gastroai.be.application.document;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.domain.rag.Chunk;
import vn.gastroai.be.domain.rag.Document;
import vn.gastroai.be.infrastructure.persistence.postgres.ChunkRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.DocumentRepository;

import java.util.List;

@Service
public class DocumentChunkingService {

    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final ChunkingService chunkingService;

    public DocumentChunkingService(
            DocumentRepository documentRepository,
            ChunkRepository chunkRepository,
            ChunkingService chunkingService
    ) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.chunkingService = chunkingService;
    }

    @Transactional
    public void chunkDocument(Long documentId) {

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy document: " + documentId
                        )
                );

        if (document.getFullText() == null
                || document.getFullText().isBlank()) {

            throw new IllegalArgumentException(
                    "Document không có fullText để chunk"
            );
        }

        // Xóa chunk cũ nếu document được chunk lại
        chunkRepository.deleteByDocumentId(documentId);

        List<Chunk> chunks =
                chunkingService.chunk(document);

        chunkRepository.saveAll(chunks);
    }
}