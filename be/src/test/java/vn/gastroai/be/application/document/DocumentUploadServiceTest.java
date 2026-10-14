package vn.gastroai.be.application.document;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.domain.rag.Document;
import vn.gastroai.be.domain.rag.DocumentStatus;
import vn.gastroai.be.infrastructure.filestorage.FileStorageService;
import vn.gastroai.be.infrastructure.persistence.postgres.DocumentRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
        verify(documentRepository, never()).saveAndFlush(any());
    }

    // Tai lieu goc khong ton tai -> ResourceNotFoundException (404), khong luu gi
    // ca.
    @Test
    void uploadNewVersionThrowsWhenDocumentNotFound() {
        when(documentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.uploadNewVersion(99L, file, null));

        verify(fileStorageService, never()).store(any());
        verify(documentRepository, never()).saveAndFlush(any());
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
        v1.setCurrentVersion(false);
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

    // ===== C3 - chan trung noi dung theo nhom phien ban =====

    @TempDir
    Path tempDir;

    // Tao file that de service tinh ma bam SHA-256 tu noi dung.
    private Path storedFile(String content) throws IOException {
        Path path = tempDir.resolve("upload.pdf");
        Files.writeString(path, content);
        return path;
    }

    // Tai lieu moi trung voi 1 tai lieu khac -> chan, bao ten + id, xoa file vua
    // luu.
    @Test
    void uploadRejectsContentDuplicatingAnotherDocument() throws IOException {
        Path stored = storedFile("noi dung trung");
        when(fileStorageService.store(file)).thenReturn(stored);
        Document existing = document(71L, DocumentStatus.DONE);
        existing.setRootId(7L);
        existing.setVersionNo(2);
        existing.setTitle("ibs.pdf");
        when(documentRepository.findByContentHashAndStatusNot(anyString(), eq(DocumentStatus.ERROR)))
                .thenReturn(List.of(existing));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.upload(file, null));

        assertTrue(ex.getMessage().contains("ibs.pdf"));
        assertTrue(ex.getMessage().contains("(id 7)"));
        assertFalse(ex.getMessage().contains("71"));
        verify(fileStorageService).delete(stored);
        verify(documentRepository, never()).saveAndFlush(any());
    }

    // Phien ban moi trung voi 1 phien ban trong CHINH nhom -> chan, bao so phien
    // ban.
    @Test
    void uploadNewVersionRejectsContentDuplicatingSameGroup() throws IOException {
        Document v1 = document(1L, DocumentStatus.DONE);
        v1.setVersionNo(1);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(v1));
        Path stored = storedFile("noi dung trung");
        when(fileStorageService.store(file)).thenReturn(stored);
        when(documentRepository.findByContentHashAndStatusNot(anyString(), eq(DocumentStatus.ERROR)))
                .thenReturn(List.of(v1));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.uploadNewVersion(1L, file, null));

        assertTrue(ex.getMessage().contains("v1"));
        verify(fileStorageService).delete(stored);
        verify(documentRepository, never()).saveAndFlush(any());
    }

    // Phien ban moi trung voi tai lieu o NHOM KHAC -> van cho tai len.
    @Test
    void uploadNewVersionAllowsContentDuplicatingOtherGroup() throws IOException {
        Document v1 = document(1L, DocumentStatus.DONE);
        v1.setVersionNo(1);
        Document other = document(50L, DocumentStatus.DONE);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(v1));
        when(fileStorageService.store(file)).thenReturn(storedFile("noi dung giong tai lieu khac"));
        when(documentRepository.findByContentHashAndStatusNot(anyString(), eq(DocumentStatus.ERROR)))
                .thenReturn(List.of(other));
        when(documentRepository.saveAndFlush(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        Document saved = service.uploadNewVersion(1L, file, null);

        assertEquals(2, saved.getVersionNo());
        assertEquals(1L, saved.getRootId());
        assertFalse(saved.isCurrentVersion());
        verify(fileStorageService, never()).delete(any());
    }

    // Tai lieu moi khong trung gi -> luu thanh v1, CHUA la ban dang dung (cho
    // markDone bat).
    @Test
    void uploadCreatesNonCurrentFirstVersionWhenNoDuplicate() throws IOException {
        when(fileStorageService.store(file)).thenReturn(storedFile("noi dung moi"));
        when(documentRepository.findByContentHashAndStatusNot(anyString(), eq(DocumentStatus.ERROR)))
                .thenReturn(List.of());
        when(documentRepository.saveAndFlush(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        Document saved = service.upload(file, null);

        assertEquals(1, saved.getVersionNo());
        assertFalse(saved.isCurrentVersion());
        assertEquals(DocumentStatus.PENDING, saved.getStatus());
    }

    // ===== C5 - hai nguoi cung tai ban moi: bao loi 409 de hieu =====

    // saveAndFlush bi rang buoc (nhom, versionNo) tu choi -> doi thanh cau than thien,
    // va file vua luu van bi xoa (khong de file mo coi).
    @Test
    void uploadNewVersionReportsFriendlyConflictWhenAnotherVersionWasJustUploaded() throws IOException {
        Document v1 = document(1L, DocumentStatus.DONE);
        v1.setVersionNo(1);
        Path stored = storedFile("noi dung ban 2");
        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(v1));
        when(fileStorageService.store(file)).thenReturn(stored);
        when(documentRepository.findByContentHashAndStatusNot(anyString(), eq(DocumentStatus.ERROR)))
                .thenReturn(List.of());
        when(documentRepository.saveAndFlush(any(Document.class)))
                .thenThrow(new DataIntegrityViolationException("uk_documents_group_version"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.uploadNewVersion(1L, file, null));

        assertTrue(ex.getMessage().contains("Vừa có người khác tải phiên bản mới"));
        verify(fileStorageService).delete(stored);
    }

    // Tai TAI LIEU MOI ma gap loi rang buoc -> giu nguyen loi goc (khong phai chuyen 2 nguoi tranh nhau).
    @Test
    void uploadKeepsOriginalExceptionForNewDocument() throws IOException {
        Path stored = storedFile("noi dung moi");
        when(fileStorageService.store(file)).thenReturn(stored);
        when(documentRepository.findByContentHashAndStatusNot(anyString(), eq(DocumentStatus.ERROR)))
                .thenReturn(List.of());
        when(documentRepository.saveAndFlush(any(Document.class)))
                .thenThrow(new DataIntegrityViolationException("loi rang buoc"));

        assertThrows(DataIntegrityViolationException.class, () -> service.upload(file, null));

        verify(fileStorageService).delete(stored);
    }
}