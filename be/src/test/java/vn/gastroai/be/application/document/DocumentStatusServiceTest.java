package vn.gastroai.be.application.document;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.gastroai.be.domain.rag.Document;
import vn.gastroai.be.domain.rag.DocumentProcessingStage;
import vn.gastroai.be.domain.rag.DocumentStatus;
import vn.gastroai.be.infrastructure.persistence.postgres.ChunkRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.DocumentRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentStatusServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ChunkRepository chunkRepository;

    @InjectMocks
    private DocumentStatusService documentStatusService;

    @Test
    void tracksEveryPipelineStageAndMarksFailure() {
        Document document = new Document();
        document.setId(42L);
        when(documentRepository.findById(42L)).thenReturn(Optional.of(document));

        documentStatusService.markProcessing(42L);
        assertEquals(DocumentStatus.PROCESSING, document.getStatus());
        assertEquals(DocumentProcessingStage.EXTRACTION, document.getProcessingStage());

        documentStatusService.markChunking(42L);
        assertEquals(DocumentProcessingStage.CHUNKING, document.getProcessingStage());

        documentStatusService.markEmbedding(42L);
        assertEquals(DocumentProcessingStage.EMBEDDING, document.getProcessingStage());

        documentStatusService.markDone(42L);
        assertEquals(DocumentStatus.DONE, document.getStatus());
        assertEquals(DocumentProcessingStage.DONE, document.getProcessingStage());

        documentStatusService.markError(42L, "Embedding failed");
        assertEquals(DocumentStatus.ERROR, document.getStatus());
        assertEquals(DocumentProcessingStage.ERROR, document.getProcessingStage());
        assertEquals("Embedding failed", document.getErrorMessage());

        verify(documentRepository, org.mockito.Mockito.times(5)).save(document);
    }

    @Test
    void listFiltersByStatusAndQuery() {
        Document d1 = new Document();
        d1.setId(1L);
        d1.setTitle("Hội chứng ruột kích thích IBS");
        d1.setStatus(DocumentStatus.DONE);

        Document d2 = new Document();
        d2.setId(2L);
        d2.setTitle("Viêm loét dạ dày");
        d2.setStatus(DocumentStatus.ERROR);

        when(documentRepository.findAllByOrderByUploadedAtDesc()).thenReturn(java.util.List.of(d1, d2));
        List<Object[]> counts = new java.util.ArrayList<>();
        counts.add(new Object[]{1L, 15L});
        when(chunkRepository.countByDocumentIdGrouped()).thenReturn(counts);

        var page = documentStatusService.list("ruot kich thich", null, 0, 10);
        assertEquals(1, page.total());
        assertEquals(1L, page.items().get(0).id());
        assertEquals(15L, page.items().get(0).chunkCount());
    }

    @Test
    void retryResetsErrorDocumentToPending() {
        Document document = new Document();
        document.setId(42L);
        document.setStatus(DocumentStatus.ERROR);
        document.setProcessingStage(DocumentProcessingStage.ERROR);
        document.setErrorMessage("Lỗi");

        when(documentRepository.findById(42L)).thenReturn(Optional.of(document));
        when(documentRepository.save(document)).thenReturn(document);

        Document retried = documentStatusService.retry(42L);
        assertEquals(DocumentStatus.PENDING, retried.getStatus());
        assertEquals(DocumentProcessingStage.PENDING, retried.getProcessingStage());
        org.junit.jupiter.api.Assertions.assertNull(retried.getErrorMessage());
    }

    @Test
    void retryThrowsWhenNotError() {
        Document document = new Document();
        document.setId(42L);
        document.setStatus(DocumentStatus.DONE);

        when(documentRepository.findById(42L)).thenReturn(Optional.of(document));

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> documentStatusService.retry(42L)
        );
    }
}