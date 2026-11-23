package vn.gastroai.be.application.document;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vn.gastroai.be.domain.rag.Document;
import vn.gastroai.be.domain.rag.DocumentProcessingStage;
import vn.gastroai.be.infrastructure.filestorage.FileStorageService;
import vn.gastroai.be.infrastructure.persistence.postgres.DocumentRepository;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DocumentGroupPrunerTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private DocumentGroupPruner pruner;

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private Document row(Long id, Long replacesId, String source) {
        Document document = new Document();
        document.setId(id);
        document.setReplacesId(replacesId);
        document.setSource(source);
        document.setProcessingStage(DocumentProcessingStage.DONE);
        return document;
    }

    // Xac nhan ban thay the: ban thay the thanh dong chinh va duoc GHI XUONG DB TRUOC khi xoa
    // dong chinh cu - xoa truoc thi ON DELETE CASCADE se cuon luon ca ban thay the.
    @Test
    void keepOnlyReRootsReplacementBeforeDeletingOldMainRow() {
        Document main = row(1L, null, "uploads/v1.pdf");
        Document replacement = row(2L, 1L, "uploads/v2.pdf");

        int deleted = pruner.keepOnly(replacement, List.of(replacement, main));

        assertEquals(1, deleted);
        assertNull(replacement.getReplacesId());
        InOrder order = inOrder(documentRepository);
        order.verify(documentRepository).saveAndFlush(replacement);
        order.verify(documentRepository).deleteAllByIdInBatch(List.of(1L));
        verify(fileStorageService).delete(Path.of("uploads/v1.pdf"));
        verify(fileStorageService, never()).delete(Path.of("uploads/v2.pdf"));
    }

    // Tai lieu dang bi go khoi chatbot thi sau khi thay the van con bi go.
    @Test
    void keepOnlyCarriesUnpublishedFlagFromOldMainRow() {
        Instant unpublishedAt = Instant.parse("2026-11-01T00:00:00Z");
        Document main = row(1L, null, "uploads/v1.pdf");
        main.setUnpublishedAt(unpublishedAt);
        Document replacement = row(2L, 1L, "uploads/v2.pdf");

        pruner.keepOnly(replacement, List.of(replacement, main));

        assertEquals(unpublishedAt, replacement.getUnpublishedAt());
    }

    // Giu dong chinh (tai ban thay the moi): chi xoa ban thay the cu, khong dung toi dong chinh.
    @Test
    void keepOnlyMainRowJustDeletesOldReplacement() {
        Document main = row(1L, null, "uploads/v1.pdf");
        Document oldReplacement = row(2L, 1L, "uploads/v2.pdf");

        pruner.keepOnly(main, List.of(oldReplacement, main));

        verify(documentRepository, never()).saveAndFlush(any());
        verify(documentRepository).deleteAllByIdInBatch(List.of(2L));
        verify(fileStorageService).delete(Path.of("uploads/v2.pdf"));
    }

    @Test
    void keepOnlyDoesNothingWhenNoOtherRow() {
        Document main = row(1L, null, "uploads/v1.pdf");

        assertEquals(0, pruner.keepOnly(main, List.of(main)));

        verify(documentRepository, never()).deleteAllByIdInBatch(anyList());
        verify(fileStorageService, never()).delete(any());
    }

    // Du lieu hong (ban thay the ma nhom khong co dong chinh cua no) -> dung lai, khong xoa gi.
    @Test
    void keepOnlyRejectsReplacementWhoseMainRowIsMissing() {
        Document replacement = row(2L, 1L, "uploads/v2.pdf");
        Document stranger = row(3L, null, "uploads/v3.pdf");

        assertThrows(IllegalStateException.class,
                () -> pruner.keepOnly(replacement, List.of(replacement, stranger)));

        verify(documentRepository, never()).deleteAllByIdInBatch(anyList());
    }

    @Test
    void deleteAllRemovesEveryRowAndFile() {
        Document main = row(1L, null, "uploads/v1.pdf");
        Document replacement = row(2L, 1L, "uploads/v2.pdf");

        pruner.deleteAll(List.of(replacement, main));

        verify(documentRepository).deleteAllByIdInBatch(List.of(2L, 1L));
        verify(fileStorageService).delete(Path.of("uploads/v1.pdf"));
        verify(fileStorageService).delete(Path.of("uploads/v2.pdf"));
    }

    // Trong giao dich: chi xoa file SAU KHI commit - rollback thi file van con.
    @Test
    void deletesFilesOnlyAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        Document main = row(1L, null, "uploads/v1.pdf");

        pruner.deleteAll(List.of(main));

        verify(fileStorageService, never()).delete(any());
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
        verify(fileStorageService).delete(Path.of("uploads/v1.pdf"));
    }
}