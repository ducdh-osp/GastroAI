package vn.gastroai.be.application.document;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.domain.rag.Document;
import vn.gastroai.be.domain.rag.DocumentStatus;
import vn.gastroai.be.infrastructure.filestorage.FileStorageService;
import vn.gastroai.be.infrastructure.persistence.postgres.DocumentRepository;

import java.nio.file.Path;
import java.util.List;
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

    @Mock
    private MultipartFile file;

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

    // ===== B1 / B6 - xoa tai lieu (xoa ca nhom phien ban) =====

    @Test
    void deleteThrowsNotFoundWhenDocumentDoesNotExist() {
        when(documentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(99L));
        verify(fileStorageService, never()).delete(any());
    }

    @Test
    void deleteRejectsDocumentStillBeingProcessed() {
        Document document = document(1L, DocumentStatus.PROCESSING);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(document));

        assertThrows(IllegalStateException.class, () -> service.delete(1L));
        verify(documentRepository, never()).deleteGroup(any());
        verify(fileStorageService, never()).delete(any());
    }

    @Test
    void deleteRemovesRowAndFileImmediatelyWhenNoTransaction() {
        Document document = document(1L, DocumentStatus.DONE);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(document));

        service.delete(1L);

        verify(documentRepository).deleteGroup(1L);
        verify(fileStorageService).delete(Path.of("uploads/phac-do.pdf"));
    }

    @Test
    void deleteWaitsForCommitBeforeRemovingFile() {
        TransactionSynchronizationManager.initSynchronization();
        Document document = document(1L, DocumentStatus.DONE);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(document));

        service.delete(1L);

        verify(fileStorageService, never()).delete(any());

        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
        verify(fileStorageService).delete(Path.of("uploads/phac-do.pdf"));
    }

    // Goi delete bang id cua 1 ban con -> xoa ca nhom va file cua MOI phien ban.
    @Test
    void deleteRemovesFilesOfAllVersions() {
        Document v1 = document(1L, DocumentStatus.DONE);
        v1.setSource("uploads/v1.pdf");
        Document v2 = document(2L, DocumentStatus.DONE);
        v2.setRootId(1L);
        v2.setSource("uploads/v2.pdf");

        when(documentRepository.findById(2L)).thenReturn(Optional.of(v2));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(v2, v1));

        service.delete(2L);

        verify(documentRepository).deleteGroup(1L);
        verify(fileStorageService).delete(Path.of("uploads/v1.pdf"));
        verify(fileStorageService).delete(Path.of("uploads/v2.pdf"));
    }

    // Chi can 1 phien ban dang xu ly la khong cho xoa ca nhom (worker dang ghi
    // chunk/embedding).
    @Test
    void deleteRejectsWhenAnyVersionProcessing() {
        Document v1 = document(1L, DocumentStatus.DONE);
        Document v2 = document(2L, DocumentStatus.PROCESSING);
        v2.setRootId(1L);

        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(v2, v1));

        assertThrows(IllegalStateException.class, () -> service.delete(1L));

        verify(documentRepository, never()).deleteGroup(any());
        verify(fileStorageService, never()).delete(any());
    }

    // ===== UC0049 - tai phien ban moi =====

    // Ban moi nhat cua nhom dang PENDING/PROCESSING thi chua cho tai ban moi len
    // (IllegalStateException -> 409). Phai chan TRUOC khi luu file, tranh file mo
    // coi.
    @Test
    void uploadNewVersionRejectsWhenPreviousVersionStillProcessing() {
        Document v1 = document(1L, DocumentStatus.PROCESSING);
        v1.setVersionNo(1);

        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(v1));

        assertThrows(IllegalStateException.class, () -> service.uploadNewVersion(1L, file, null));

        verify(fileStorageService, never()).store(any());
        verify(documentRepository, never()).save(any());
    }

    // Tai lieu goc khong ton tai -> ResourceNotFoundException (404), khong luu gi
    // ca.
    @Test
    void uploadNewVersionThrowsWhenDocumentNotFound() {
        when(documentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.uploadNewVersion(99L, file, null));

        verify(fileStorageService, never()).store(any());
        verify(documentRepository, never()).save(any());
    }

    // ===== C2 - xoa rieng mot phien ban =====

    // Phien ban thuoc nhom khac -> coi nhu khong tim thay (404), khong xoa gi ca.
    @Test
    void deleteVersionRejectsVersionFromDifferentGroup() {
        Document v1 = document(1L, DocumentStatus.DONE);
        Document other = document(20L, DocumentStatus.DONE);
        other.setRootId(10L);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));
        when(documentRepository.findById(20L)).thenReturn(Optional.of(other));

        assertThrows(ResourceNotFoundException.class, () -> service.deleteVersion(1L, 20L));

        verify(documentRepository, never()).delete(any());
        verify(documentRepository, never()).deleteGroup(any());
        verify(fileStorageService, never()).delete(any());
    }

    // Ban dang xu ly -> 409, worker dang ghi du lieu cho ban nay.
    @Test
    void deleteVersionRejectsVersionStillProcessing() {
        Document v1 = document(1L, DocumentStatus.DONE);
        v1.setCurrentVersion(true);
        Document v2 = document(2L, DocumentStatus.PROCESSING);
        v2.setRootId(1L);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));
        when(documentRepository.findById(2L)).thenReturn(Optional.of(v2));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(v2, v1));

        assertThrows(IllegalStateException.class, () -> service.deleteVersion(1L, 2L));

        verify(documentRepository, never()).delete(any());
        verify(fileStorageService, never()).delete(any());
    }

    // Ban dang dung -> 409, tranh chatbot mat tai lieu ma admin khong hay.
    @Test
    void deleteVersionRejectsCurrentVersion() {
        Document v1 = document(1L, DocumentStatus.DONE);
        v1.setCurrentVersion(false);
        Document v2 = document(2L, DocumentStatus.DONE);
        v2.setRootId(1L);
        v2.setCurrentVersion(true);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));
        when(documentRepository.findById(2L)).thenReturn(Optional.of(v2));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(v2, v1));

        assertThrows(IllegalStateException.class, () -> service.deleteVersion(1L, 2L));

        verify(documentRepository, never()).delete(any());
        verify(fileStorageService, never()).delete(any());
    }

    // Ban goc khi nhom con ban khac -> 409, vi ON DELETE CASCADE se xoa ca nhom.
    @Test
    void deleteVersionRejectsRootWhileOtherVersionsExist() {
        Document v1 = document(1L, DocumentStatus.DONE);
        Document v2 = document(2L, DocumentStatus.DONE);
        v2.setRootId(1L);
        v2.setCurrentVersion(true);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(v2, v1));

        assertThrows(IllegalStateException.class, () -> service.deleteVersion(1L, 1L));

        verify(documentRepository, never()).delete(any());
        verify(fileStorageService, never()).delete(any());
    }

    // Xoa ban le thanh cong: chi xoa dung 1 dong va dung 1 file, khong dong den
    // nhom.
    @Test
    void deleteVersionRemovesOnlyThatVersion() {
        Document v1 = document(1L, DocumentStatus.DONE);
        v1.setSource("uploads/v1.pdf");
        Document v2 = document(2L, DocumentStatus.DONE);
        v2.setRootId(1L);
        v2.setCurrentVersion(true);
        v2.setSource("uploads/v2.pdf");
        Document v3 = document(3L, DocumentStatus.DONE);
        v3.setRootId(1L);
        v3.setCurrentVersion(false);
        v3.setSource("uploads/v3.pdf");
        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));
        when(documentRepository.findById(3L)).thenReturn(Optional.of(v3));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(v3, v2, v1));

        service.deleteVersion(1L, 3L);

        verify(documentRepository).delete(v3);
        verify(documentRepository, never()).deleteGroup(any());
        verify(fileStorageService).delete(Path.of("uploads/v3.pdf"));
        verify(fileStorageService, never()).delete(Path.of("uploads/v1.pdf"));
        verify(fileStorageService, never()).delete(Path.of("uploads/v2.pdf"));
    }

    // Nhom chi con 1 ban -> xoa luon ca tai lieu (khong de lai nhom rong).
    @Test
    void deleteVersionOfLastRemainingVersionDeletesWholeDocument() {
        Document v1 = document(1L, DocumentStatus.DONE);
        v1.setCurrentVersion(true);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(v1));

        service.deleteVersion(1L, 1L);

        verify(documentRepository).deleteGroup(1L);
        verify(fileStorageService).delete(Path.of("uploads/phac-do.pdf"));
    }
}