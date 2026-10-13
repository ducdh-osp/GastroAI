package vn.gastroai.be.application.document;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.domain.rag.Document;
import vn.gastroai.be.domain.rag.DocumentProcessingStage;
import vn.gastroai.be.domain.rag.DocumentStatus;
import vn.gastroai.be.infrastructure.persistence.postgres.ChunkRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.DocumentRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
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

    // ===== UC0049 - phien ban tai lieu =====

    // Tao 1 phien ban thuoc nhom (rootId = id cua ban goc).
    private Document version(long id, Long rootId, int versionNo, boolean current, DocumentStatus status) {
        Document document = new Document();
        document.setId(id);
        document.setRootId(rootId);
        document.setVersionNo(versionNo);
        document.setCurrentVersion(current);
        document.setStatus(status);
        return document;
    }

    // Ban 2 xu ly xong nhung chua la ban hien hanh -> markDone phai chuyen "hien hanh" sang ban 2.
    // clearCurrent PHAI chay truoc setCurrentVersion/save, neu khong index
    // uk_documents_group_current bi vi pham (2 ban cung la hien hanh trong 1 nhom).
    @Test
    void markDoneSwitchesCurrentVersionForNonCurrentDocument() {
        Document v2 = version(2L, 1L, 2, false, DocumentStatus.PROCESSING);
        when(documentRepository.findById(2L)).thenReturn(Optional.of(v2));

        documentStatusService.markDone(2L);

        assertEquals(DocumentStatus.DONE, v2.getStatus());
        assertTrue(v2.isCurrentVersion());

        InOrder order = inOrder(documentRepository);
        order.verify(documentRepository).clearCurrent(1L);
        order.verify(documentRepository).save(v2);
    }

    // Ban 1 (hoac ban da la hien hanh) xong -> khong dong vao co hien hanh cua ai ca.
    @Test
    void markDoneLeavesAlreadyCurrentDocumentAlone() {
        Document v1 = version(1L, null, 1, true, DocumentStatus.PROCESSING);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));

        documentStatusService.markDone(1L);

        assertEquals(DocumentStatus.DONE, v1.getStatus());
        assertTrue(v1.isCurrentVersion());
        verify(documentRepository, never()).clearCurrent(any());
        verify(documentRepository).save(v1);
    }

    // Phien ban thuoc nhom khac (rootId khac) -> khong duoc kich hoat, coi nhu khong tim thay.
    @Test
    void activateVersionRejectsVersionFromDifferentGroup() {
        Document v1 = version(1L, null, 1, true, DocumentStatus.DONE);
        Document other = version(20L, 10L, 2, false, DocumentStatus.DONE);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));
        when(documentRepository.findById(20L)).thenReturn(Optional.of(other));

        assertThrows(ResourceNotFoundException.class,
                () -> documentStatusService.activateVersion(1L, 20L));

        verify(documentRepository, never()).clearCurrent(any());
        verify(documentRepository, never()).save(any());
        assertFalse(other.isCurrentVersion());
    }

    // Ban dich chua xu ly xong (vd ERROR) -> khong duoc dung lam ban hien hanh.
    @Test
    void activateVersionRejectsNonDoneTarget() {
        Document v1 = version(1L, null, 1, true, DocumentStatus.DONE);
        Document v2 = version(2L, 1L, 2, false, DocumentStatus.ERROR);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));
        when(documentRepository.findById(2L)).thenReturn(Optional.of(v2));

        assertThrows(IllegalStateException.class,
                () -> documentStatusService.activateVersion(1L, 2L));

        verify(documentRepository, never()).clearCurrent(any());
        verify(documentRepository, never()).save(any());
        assertFalse(v2.isCurrentVersion());
    }

    // Truong hop thanh cong: bo co hien hanh cua ca nhom TRUOC, bat ban dich SAU, roi save.
    @Test
    void activateVersionSwitchesCurrentVersion() {
        Document v1 = version(1L, null, 1, true, DocumentStatus.DONE);
        Document v2 = version(2L, 1L, 2, false, DocumentStatus.DONE);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));
        when(documentRepository.findById(2L)).thenReturn(Optional.of(v2));

        documentStatusService.activateVersion(1L, 2L);

        assertTrue(v2.isCurrentVersion());

        InOrder order = inOrder(documentRepository);
        order.verify(documentRepository).clearCurrent(1L);
        order.verify(documentRepository).save(v2);
    }

    // ===== B5 - danh sach tai lieu: moi tai lieu 1 dong =====

    // Tai lieu A co 3 phien ban -> chi hien 1 dong (ban moi nhat = ban 3), kem so ban dang dung
    // (ban 2) va tong so phien ban (3).
    @Test
    void listShowsOneRowPerDocumentWithLatestVersion() {
        Document v1 = version(1L, null, 1, false, DocumentStatus.DONE);
        v1.setTitle("Phác đồ HP");
        Document v2 = version(2L, 1L, 2, true, DocumentStatus.DONE);
        v2.setTitle("Phác đồ HP");
        Document v3 = version(3L, 1L, 3, false, DocumentStatus.PROCESSING);
        v3.setTitle("Phác đồ HP");

        when(documentRepository.findAllByOrderByUploadedAtDesc()).thenReturn(List.of(v3, v2, v1));
        when(chunkRepository.countByDocumentIdGrouped()).thenReturn(new ArrayList<>());

        var page = documentStatusService.list(null, null, 0, 10);

        assertEquals(1, page.total());
        assertEquals(1, page.items().size());
        var item = page.items().get(0);
        assertEquals(3L, item.id());
        assertEquals(3, item.versionNo());
        assertEquals(2, item.currentVersionNo());
        assertEquals(3, item.versionCount());
    }

    // Tai lieu da bi go (khong ban nao la hien hanh) -> currentVersionNo la null.
    @Test
    void listReturnsNullCurrentVersionNoWhenNoVersionIsCurrent() {
        Document v1 = version(1L, null, 1, false, DocumentStatus.DONE);
        Document v2 = version(2L, 1L, 2, false, DocumentStatus.DONE);
        Document v3 = version(3L, 1L, 3, false, DocumentStatus.DONE);

        when(documentRepository.findAllByOrderByUploadedAtDesc()).thenReturn(List.of(v3, v2, v1));
        when(chunkRepository.countByDocumentIdGrouped()).thenReturn(new ArrayList<>());

        var page = documentStatusService.list(null, null, 0, 10);

        assertEquals(1, page.total());
        assertNull(page.items().get(0).currentVersionNo());
        assertEquals(3, page.items().get(0).versionCount());
    }
}