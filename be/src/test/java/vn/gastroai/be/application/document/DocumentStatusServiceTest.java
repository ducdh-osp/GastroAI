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
}