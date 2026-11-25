package vn.gastroai.be.application.document;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.multipart.MultipartFile;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.domain.rag.Document;
import vn.gastroai.be.domain.rag.DocumentProcessingStage;
import vn.gastroai.be.domain.rag.DocumentStatus;
import vn.gastroai.be.infrastructure.filestorage.FileStorageService;
import vn.gastroai.be.infrastructure.persistence.postgres.DocumentRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Mock
    private DocumentGroupPruner groupPruner;

    @InjectMocks
    private DocumentUploadService service;

    private Document document(Long id, DocumentStatus status) {
        Document document = new Document();
        document.setId(id);
        document.setTitle("phac-do.pdf");
        document.setSource("uploads/phac-do.pdf");
        document.setProcessingStage(stageOf(status));
        return document;
    }

    /** Bước xử lý đại diện cho 1 trạng thái - Document chỉ lưu bước, trạng thái suy ra từ đó. */
    private static DocumentProcessingStage stageOf(DocumentStatus status) {
        return switch (status) {
            case PENDING -> DocumentProcessingStage.PENDING;
            case PROCESSING -> DocumentProcessingStage.EXTRACTION;
            case DONE -> DocumentProcessingStage.DONE;
            case ERROR -> DocumentProcessingStage.ERROR;
        };
    }

    // Ban dang dung cho AI: da xu ly xong VA da duoc Bien tap xac nhan noi dung.
    private Document live(Long id) {
        Document document = document(id, DocumentStatus.DONE);
        document.setContentApprovedAt(java.time.Instant.now());
        return document;
    }

    // ===== B1 / B6 - xoa tai lieu (dong chinh + ban thay the) =====

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
        verify(groupPruner, never()).deleteAll(any());
    }

    // Goi delete bang id cua ban thay the -> xoa ca tai lieu: dong chinh + ban thay the.
    @Test
    void deleteRemovesMainRowAndReplacement() {
        Document v1 = document(1L, DocumentStatus.DONE);
        Document v2 = document(2L, DocumentStatus.DONE);
        v2.setReplacesId(1L);
        when(documentRepository.findById(2L)).thenReturn(Optional.of(v2));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(v2, v1));

        service.delete(2L);

        verify(groupPruner).deleteAll(List.of(v2, v1));
    }

    // Ban thay the dang xu ly thi chua cho xoa tai lieu (worker dang ghi chunk/embedding).
    @Test
    void deleteRejectsWhileReplacementProcessing() {
        Document v1 = document(1L, DocumentStatus.DONE);
        Document v2 = document(2L, DocumentStatus.PROCESSING);
        v2.setReplacesId(1L);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(v2, v1));

        assertThrows(IllegalStateException.class, () -> service.delete(1L));

        verify(groupPruner, never()).deleteAll(any());
    }

    // ===== UC0049 / SCR-AD-04 §3.2 - tai ban thay the =====

    // Tai lieu dang co dong PENDING/PROCESSING thi chua cho tai ban thay the
    // (IllegalStateException -> 409). Phai chan TRUOC khi luu file, tranh file mo coi.
    @Test
    void uploadReplacementRejectsWhilePreviousUploadStillProcessing() {
        Document v1 = document(1L, DocumentStatus.PROCESSING);

        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(v1));

        assertThrows(IllegalStateException.class, () -> service.uploadReplacement(1L, file));

        verify(fileStorageService, never()).store(any());
        verify(documentRepository, never()).saveAndFlush(any());
    }

    // Tai lieu goc khong ton tai -> ResourceNotFoundException (404), khong luu gi
    // ca.
    @Test
    void uploadReplacementThrowsWhenDocumentNotFound() {
        when(documentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.uploadReplacement(99L, file));

        verify(fileStorageService, never()).store(any());
        verify(documentRepository, never()).saveAndFlush(any());
    }

    // ===== C3 - chan trung noi dung =====

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
        existing.setReplacesId(7L);
        existing.setTitle("ibs.pdf");
        when(documentRepository.findByContentHashAndProcessingStageNot(anyString(), eq(DocumentProcessingStage.ERROR)))
                .thenReturn(List.of(existing));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.upload(file, null, "Tài liệu test"));

        assertTrue(ex.getMessage().contains("ibs.pdf"));
        assertTrue(ex.getMessage().contains("(id 7)"));
        assertFalse(ex.getMessage().contains("71"));
        verify(fileStorageService).delete(stored);
        verify(documentRepository, never()).saveAndFlush(any());
    }

    // Ban thay the trung noi dung voi ban dang dung cua CHINH tai lieu -> chan.
    @Test
    void uploadReplacementRejectsContentSameAsRowInUse() throws IOException {
        Document v1 = live(1L);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(v1));
        Path stored = storedFile("noi dung trung");
        when(fileStorageService.store(file)).thenReturn(stored);
        when(documentRepository.findByContentHashAndProcessingStageNot(anyString(), eq(DocumentProcessingStage.ERROR)))
                .thenReturn(List.of(v1));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.uploadReplacement(1L, file));

        assertTrue(ex.getMessage().contains("trùng với bản đang dùng"));
        verify(fileStorageService).delete(stored);
        verify(documentRepository, never()).saveAndFlush(any());
    }

    // Ban thay the trung voi 1 tai lieu KHAC -> van cho tai len.
    @Test
    void uploadReplacementAllowsContentOfAnotherDocument() throws IOException {
        Document v1 = live(1L);
        Document other = document(50L, DocumentStatus.DONE);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(v1));
        when(fileStorageService.store(file)).thenReturn(storedFile("noi dung giong tai lieu khac"));
        when(documentRepository.findByContentHashAndProcessingStageNot(anyString(), eq(DocumentProcessingStage.ERROR)))
                .thenReturn(List.of(other));
        when(documentRepository.saveAndFlush(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        Document saved = service.uploadReplacement(1L, file);
        assertEquals(1L, saved.getReplacesId());
        assertFalse(saved.isInUse());
        verify(fileStorageService, never()).delete(any());
    }

    // Tai lieu moi khong trung gi -> luu PENDING, chatbot CHUA dung (cho Bien tap xac nhan).
    @Test
    void uploadCreatesDocumentNotYetInUse() throws IOException {
        when(fileStorageService.store(file)).thenReturn(storedFile("noi dung moi"));
        when(documentRepository.findByContentHashAndProcessingStageNot(anyString(), eq(DocumentProcessingStage.ERROR)))
                .thenReturn(List.of());
        when(documentRepository.saveAndFlush(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        Document saved = service.upload(file, null, "Tài liệu test");
        assertFalse(saved.isInUse());
        assertEquals(DocumentStatus.PENDING, saved.getStatus());
    }

    // ===== C5 - hai nguoi cung tai ban moi: bao loi 409 de hieu =====

    // saveAndFlush bi rang buoc uk_documents_one_replacement tu choi -> doi thanh cau than
    // thien, va file vua luu van bi xoa (khong de file mo coi).
    @Test
    void uploadReplacementReportsFriendlyConflictWhenSomeoneJustUploadedOne() throws IOException {
        Document v1 = live(1L);
        Path stored = storedFile("noi dung ban 2");
        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(v1));
        when(fileStorageService.store(file)).thenReturn(stored);
        when(documentRepository.findByContentHashAndProcessingStageNot(anyString(), eq(DocumentProcessingStage.ERROR)))
                .thenReturn(List.of());
        when(documentRepository.saveAndFlush(any(Document.class)))
                .thenThrow(new DataIntegrityViolationException("uk_documents_group_version"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.uploadReplacement(1L, file));

        assertTrue(ex.getMessage().contains("Vừa có người khác tải bản thay thế"));
        verify(fileStorageService).delete(stored);
    }

    // Tai TAI LIEU MOI ma gap loi rang buoc -> giu nguyen loi goc (khong phai
    // chuyen 2 nguoi tranh nhau).
    @Test
    void uploadKeepsOriginalExceptionForNewDocument() throws IOException {
        Path stored = storedFile("noi dung moi");
        when(fileStorageService.store(file)).thenReturn(stored);
        when(documentRepository.findByContentHashAndProcessingStageNot(anyString(), eq(DocumentProcessingStage.ERROR)))
                .thenReturn(List.of());
        when(documentRepository.saveAndFlush(any(Document.class)))
                .thenThrow(new DataIntegrityViolationException("loi rang buoc"));

        assertThrows(DataIntegrityViolationException.class, () -> service.upload(file, null, "Tài liệu test"));

        verify(fileStorageService).delete(stored);
    }

    // ===== C8 - tai ban thay the thanh cong =====

    // Goi bang id cua ban thay the cu (id 2): ban moi tro ve dong chinh (id 1), chua duoc
    // chatbot dung, tro dung file vua luu; ban thay the cu chua xac nhan bi bo.
    @Test
    void uploadReplacementPointsToRowInUseAndDropsOldReplacement() throws IOException {
        Document v1 = live(1L);
        // Ban thay the truoc chua duoc xac nhan -> bi bo, chi giu ban dang dung + ban moi.
        Document v2 = document(2L, DocumentStatus.DONE);
        v2.setReplacesId(1L);
        Path stored = storedFile("noi dung ban 3");
        when(documentRepository.findById(2L)).thenReturn(Optional.of(v2));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(v2, v1));
        when(fileStorageService.store(file)).thenReturn(stored);
        when(documentRepository.findByContentHashAndProcessingStageNot(anyString(), eq(DocumentProcessingStage.ERROR)))
                .thenReturn(List.of());
        when(documentRepository.saveAndFlush(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        Document saved = service.uploadReplacement(2L, file);
        assertEquals(1L, saved.getReplacesId());
        assertFalse(saved.isInUse());
        assertEquals(DocumentStatus.PENDING, saved.getStatus());
        assertEquals(stored.toString(), saved.getSource());
        verify(groupPruner).keepOnly(v1, List.of(v2, v1));
        verify(groupPruner, never()).deleteAll(any());
        verify(fileStorageService, never()).delete(any());
    }

    // SCR-AD-04 §3.2: tai lieu chua tung duoc dung cho AI (vd loi xu ly) -> thay luon:
    // xoa ban cu, ban moi thanh tai lieu doc lap (dong goc), van giu ten + URL.
    @Test
    void replacingDocumentNeverUsedByAiDeletesOldRowRightAway() throws IOException {
        Document v1 = document(1L, DocumentStatus.ERROR);
        v1.setTitle("Trào ngược dạ dày");
        v1.setSourceUrl("https://moh.gov.vn/gerd");
        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(v1));
        when(fileStorageService.store(file)).thenReturn(storedFile("noi dung sua loi"));
        when(documentRepository.findByContentHashAndProcessingStageNot(anyString(), eq(DocumentProcessingStage.ERROR)))
                .thenReturn(List.of());
        when(documentRepository.saveAndFlush(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        Document saved = service.uploadReplacement(1L, file);

        verify(groupPruner).deleteAll(List.of(v1));
        verify(groupPruner, never()).keepOnly(any(), any());
        assertNull(saved.getReplacesId());
        assertEquals("Trào ngược dạ dày", saved.getTitle());
        assertEquals("https://moh.gov.vn/gerd", saved.getSourceUrl());
    }

    @Test
    void uploadUsesGivenTitleInsteadOfFileName() throws IOException {
        when(fileStorageService.store(file)).thenReturn(storedFile("noi dung moi"));
        when(documentRepository.findByContentHashAndProcessingStageNot(anyString(), eq(DocumentProcessingStage.ERROR)))
                .thenReturn(List.of());
        when(documentRepository.saveAndFlush(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        Document saved = service.upload(file, null, "  Trào ngược   dạ dày  ");

        assertEquals("Trào ngược dạ dày", saved.getTitle());
    }

    // De trong ten -> dung ten file nhu truoc day.
    @Test
    void uploadFallsBackToFileNameWhenTitleBlank() throws IOException {
        when(file.getOriginalFilename()).thenReturn("trao-nguoc.pdf");
        when(fileStorageService.store(file)).thenReturn(storedFile("noi dung moi"));
        when(documentRepository.findByContentHashAndProcessingStageNot(anyString(), eq(DocumentProcessingStage.ERROR)))
                .thenReturn(List.of());
        when(documentRepository.saveAndFlush(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        Document saved = service.upload(file, null, "   ");

        assertEquals("trao-nguoc.pdf", saved.getTitle());
    }

    // Ten qua 255 ky tu -> bao loi NGAY, chua luu file (khong de lai file mo coi).
    @Test
    void uploadRejectsTooLongTitleBeforeStoringFile() {
        assertThrows(IllegalArgumentException.class,
                () -> service.upload(file, null, "a".repeat(256)));

        verify(fileStorageService, never()).store(any());
        verify(documentRepository, never()).saveAndFlush(any());
    }

    // Phien ban moi giu ten hien tai cua tai lieu, khong lay ten file moi.
    @Test
    void uploadReplacementKeepsDocumentTitle() throws IOException {
        Document v1 = live(1L);
        v1.setTitle("Trào ngược dạ dày");
        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(v1));
        when(fileStorageService.store(file)).thenReturn(storedFile("noi dung ban 2"));
        when(documentRepository.findByContentHashAndProcessingStageNot(anyString(), eq(DocumentProcessingStage.ERROR)))
                .thenReturn(List.of());
        when(documentRepository.saveAndFlush(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        Document saved = service.uploadReplacement(1L, file);

        assertEquals("Trào ngược dạ dày", saved.getTitle());
    }

    @Test
    void uploadReplacementKeepsDocumentSourceUrl() throws IOException {
        Document v1 = live(1L);
        v1.setSourceUrl("https://moh.gov.vn/gerd");
        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(v1));
        when(fileStorageService.store(file)).thenReturn(storedFile("noi dung ban 2"));
        when(documentRepository.findByContentHashAndProcessingStageNot(anyString(), eq(DocumentProcessingStage.ERROR)))
                .thenReturn(List.of());
        when(documentRepository.saveAndFlush(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        Document saved = service.uploadReplacement(1L, file);

        assertEquals("https://moh.gov.vn/gerd", saved.getSourceUrl());
    }
}