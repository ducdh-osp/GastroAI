package vn.gastroai.be.application.document;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.domain.rag.Document;
import vn.gastroai.be.domain.rag.DocumentStatus;
import vn.gastroai.be.infrastructure.filestorage.FileStorageService;
import vn.gastroai.be.infrastructure.persistence.postgres.DocumentRepository;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentUploadServiceTest {

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private DocumentUploadService service;

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private Document document(Long id, DocumentStatus status) {
        Document document = new Document();
        document.setId(id);
        document.setTitle("phac-do.pdf");
        document.setSource("uploads/phac-do.pdf");
        document.setStatus(status);
        return document;
    }

    @Test
    void deleteThrowsNotFoundWhenDocumentDoesNotExist() {
        when(documentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(99L));
        verify(fileStorageService, never()).delete(any());
    }

    @Test
    void deleteRejectsDocumentStillBeingProcessed() {
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document(1L, DocumentStatus.PROCESSING)));

        assertThrows(IllegalStateException.class, () -> service.delete(1L));
        verify(documentRepository, never()).delete(any(Document.class));
        verify(fileStorageService, never()).delete(any());
    }

    @Test
    void deleteRemovesRowAndFileImmediatelyWhenNoTransaction() {
        Document document = document(1L, DocumentStatus.DONE);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));

        service.delete(1L);

        verify(documentRepository).delete(document);
        verify(fileStorageService).delete(Path.of("uploads/phac-do.pdf"));
    }

    @Test
    void deleteWaitsForCommitBeforeRemovingFile() {
        TransactionSynchronizationManager.initSynchronization();
        Document document = document(1L, DocumentStatus.DONE);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));

        service.delete(1L);

        verify(fileStorageService, never()).delete(any());

        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
        verify(fileStorageService).delete(Path.of("uploads/phac-do.pdf"));
    }
}