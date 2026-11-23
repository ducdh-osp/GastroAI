package vn.gastroai.be.application.document;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.gastroai.be.api.document.DocumentReviewResponse;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.domain.rag.Chunk;
import vn.gastroai.be.domain.rag.Document;
import vn.gastroai.be.domain.rag.DocumentProcessingStage;
import vn.gastroai.be.domain.rag.DocumentStatus;
import vn.gastroai.be.infrastructure.filestorage.FileStorageService;
import vn.gastroai.be.infrastructure.persistence.postgres.ChunkRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.DocumentRepository;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentReviewServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private ChunkRepository chunkRepository;
    @Mock private FileStorageService fileStorageService;

    @InjectMocks private DocumentReviewService service;

    private Document document(String source) {
        Document document = new Document();
        document.setId(7L);
        document.setTitle("Phác đồ HP");
        document.setProcessingStage(DocumentProcessingStage.DONE);
        document.setSource(source);
        document.setFullText("Toàn bộ chữ đã trích");
        return document;
    }

    private Chunk chunk(int index, String content) {
        Chunk chunk = new Chunk();
        chunk.setChunkIndex(index);
        chunk.setContent(content);
        return chunk;
    }

    // PDF xem bang iframe nen khong can gui ca full text nang ve FE.
    @Test
    void pdfReviewHasOriginalFileAndChunksButNoFullText() {
        when(documentRepository.findById(7L)).thenReturn(Optional.of(document("uploads/a.pdf")));
        when(fileStorageService.findStored("uploads/a.pdf")).thenReturn(Optional.of(Path.of("uploads/a.pdf")));
        when(chunkRepository.findByDocumentIdOrderByChunkIndex(7L))
                .thenReturn(List.of(chunk(0, "Đoạn 1"), chunk(1, "Đoạn 2")));

        DocumentReviewResponse review = service.review(7L);

        assertEquals("PDF", review.originalFileType());
        assertNull(review.fullText());
        assertEquals(2, review.chunks().size());
        assertEquals("Đoạn 2", review.chunks().get(1).content());
    }

    // DOCX trinh duyet khong xem duoc -> gui chu da trich de doi chieu.
    @Test
    void docxReviewSendsExtractedText() {
        when(documentRepository.findById(7L)).thenReturn(Optional.of(document("uploads/a.docx")));
        when(fileStorageService.findStored("uploads/a.docx")).thenReturn(Optional.of(Path.of("uploads/a.docx")));
        when(chunkRepository.findByDocumentIdOrderByChunkIndex(7L)).thenReturn(List.of());

        DocumentReviewResponse review = service.review(7L);

        assertEquals("DOCX", review.originalFileType());
        assertEquals("Toàn bộ chữ đã trích", review.fullText());
    }

    // Tai lieu mau khong co file goc -> van xem duoc chu da trich, nhung khong mo file duoc.
    @Test
    void documentWithoutOriginalFileFallsBackToText() {
        when(documentRepository.findById(7L)).thenReturn(Optional.of(document(null)));
        when(fileStorageService.findStored(null)).thenReturn(Optional.empty());
        when(chunkRepository.findByDocumentIdOrderByChunkIndex(7L)).thenReturn(List.of());

        DocumentReviewResponse review = service.review(7L);

        assertNull(review.originalFileType());
        assertEquals("Toàn bộ chữ đã trích", review.fullText());
        assertThrows(ResourceNotFoundException.class, () -> service.originalFile(7L));
    }
}