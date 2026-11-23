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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import vn.gastroai.be.infrastructure.persistence.postgres.DocumentGroupRow;
import vn.gastroai.be.infrastructure.persistence.postgres.DocumentListRow;

@ExtendWith(MockitoExtension.class)
class DocumentStatusServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ChunkRepository chunkRepository;

    @Mock
    private DocumentGroupPruner groupPruner;

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
    void retryResetsErrorDocumentToPending() {
        Document document = new Document();
        document.setId(42L);
        document.setProcessingStage(DocumentProcessingStage.ERROR);
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
        document.setProcessingStage(DocumentProcessingStage.DONE);

        when(documentRepository.findById(42L)).thenReturn(Optional.of(document));

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> documentStatusService.retry(42L));
    }

    // ===== SCR-AD-04 §3.2 - dong chinh + ban thay the =====

    // replacesId = null: dong chinh; co gia tri: ban thay the cho dong chinh do.
    // Dong chinh DONE coi nhu Bien tap da xac nhan (tuc chatbot dang dung, neu khong bi go);
    // ban thay the thi khong bao gio da xac nhan (ck_documents_replacement_pending).
    private Document row(long id, Long replacesId, DocumentStatus status) {
        Document document = new Document();
        document.setId(id);
        document.setReplacesId(replacesId);
        document.setProcessingStage(stageOf(status));
        if (status == DocumentStatus.DONE && replacesId == null) {
            document.setContentApprovedAt(Instant.now());
        }
        return document;
    }

    // ===== SCR-AD-04 §3.1b - Bien tap xac nhan noi dung truoc khi chatbot dung =====

    // Xu ly ky thuat xong chua duoc dung ngay: van cho xac nhan.
    @Test
    void markDoneWaitsForContentApprovalBeforeAiUsesIt() {
        Document v2 = row(2L, 1L, DocumentStatus.PROCESSING);
        when(documentRepository.findById(2L)).thenReturn(Optional.of(v2));

        documentStatusService.markDone(2L);

        assertEquals(DocumentStatus.DONE, v2.getStatus());
        assertFalse(v2.isInUse());
        assertNull(v2.getContentApprovedAt());
        verify(documentRepository).save(v2);
    }

    // Gia lap DocumentGroupPruner.keepOnly: ban thay the thanh dong chinh, mang co "da go".
    private void pruneReplacingMainRow(Document replacement, Document main) {
        List<Document> group = List.of(replacement, main);
        when(documentRepository.findGroup(main.getId())).thenReturn(group);
        doAnswer(invocation -> {
            replacement.setUnpublishedAt(main.getUnpublishedAt());
            replacement.setReplacesId(null);
            return 1;
        }).when(groupPruner).keepOnly(replacement, group);
    }

    // Xac nhan ban thay the -> ban cu bi xoa vinh vien (keepOnly) TRUOC, roi ban moi moi duoc
    // danh dau da xac nhan + chatbot dung.
    @Test
    void approvingReplacementDeletesOldRowAndPutsItInUse() {
        Document v1 = row(1L, null, DocumentStatus.DONE);
        Document v2 = row(2L, 1L, DocumentStatus.DONE);
        when(documentRepository.findById(2L)).thenReturn(Optional.of(v2));
        pruneReplacingMainRow(v2, v1);

        boolean replaced = documentStatusService.approveContent(2L, "bientap@gastroai.vn");

        assertTrue(replaced);
        assertNull(v2.getReplacesId());
        assertNotNull(v2.getContentApprovedAt());
        assertEquals("bientap@gastroai.vn", v2.getContentApprovedBy());
        assertTrue(v2.isInUse());
        InOrder order = inOrder(groupPruner, documentRepository);
        order.verify(groupPruner).keepOnly(v2, List.of(v2, v1));
        order.verify(documentRepository).save(v2);
    }

    // Truy van findGroup se flush target -> luc do target chua duoc mang co "da xac nhan"
    // (ban thay the da xac nhan vi pham ck_documents_replacement_pending).
    @Test
    void approvingReplacementLoadsGroupBeforeMarkingApproved() {
        Document v1 = row(1L, null, DocumentStatus.DONE);
        Document v2 = row(2L, 1L, DocumentStatus.DONE);
        when(documentRepository.findById(2L)).thenReturn(Optional.of(v2));
        when(documentRepository.findGroup(1L)).thenAnswer(invocation -> {
            assertNull(v2.getContentApprovedAt());
            assertFalse(v2.isInUse());
            return List.of(v2, v1);
        });

        documentStatusService.approveContent(2L, "bientap@gastroai.vn");

        verify(documentRepository).findGroup(1L);
    }

    // Tai lieu moi (chua co ban nao dung cho AI): xac nhan khong xoa gi ca.
    @Test
    void approvingNewDocumentDeletesNothing() {
        Document v1 = row(1L, null, DocumentStatus.DONE);
        v1.setContentApprovedAt(null);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));

        boolean replaced = documentStatusService.approveContent(1L, "bientap@gastroai.vn");

        assertFalse(replaced);
        verify(groupPruner, never()).keepOnly(any(), any());
        verify(documentRepository, never()).findGroup(any());
        assertTrue(v1.isInUse());
        verify(documentRepository).save(v1);
    }

    // Tai lieu dang bi go -> xac nhan ban thay the xong van khong tu dua len chatbot.
    @Test
    void approveContentKeepsUnpublishedDocumentOffline() {
        Document main = row(1L, null, DocumentStatus.DONE);
        main.setUnpublishedAt(Instant.now());
        Document v2 = row(2L, 1L, DocumentStatus.DONE);
        when(documentRepository.findById(2L)).thenReturn(Optional.of(v2));
        pruneReplacingMainRow(v2, main);

        documentStatusService.approveContent(2L, "bientap@gastroai.vn");

        assertNotNull(v2.getContentApprovedAt());
        assertNotNull(v2.getUnpublishedAt());
        assertFalse(v2.isInUse());
    }

    @Test
    void approveContentRejectsAlreadyApprovedRow() {
        Document v1 = row(1L, null, DocumentStatus.DONE);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));

        assertThrows(IllegalStateException.class,
                () -> documentStatusService.approveContent(1L, "bientap@gastroai.vn"));

        verify(documentRepository, never()).save(any());
    }

    @Test
    void approveContentRejectsRowNotYetProcessed() {
        Document v2 = row(2L, 1L, DocumentStatus.PROCESSING);
        when(documentRepository.findById(2L)).thenReturn(Optional.of(v2));

        assertThrows(IllegalStateException.class,
                () -> documentStatusService.approveContent(2L, "bientap@gastroai.vn"));

        verify(groupPruner, never()).keepOnly(any(), any());
        verify(documentRepository, never()).save(any());
    }

    // ===== UC0050 - go / dang lai =====

    // Go: chi dong chinh moi co the dang duoc dung; goi bang id ban thay the cung go dong chinh.
    @Test
    void unpublishTakesMainRowOffline() {
        Document main = row(1L, null, DocumentStatus.DONE);
        Document replacement = row(2L, 1L, DocumentStatus.PENDING);
        when(documentRepository.findById(2L)).thenReturn(Optional.of(replacement));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(main));

        documentStatusService.unpublish(2L);

        assertFalse(main.isInUse());
        assertNotNull(main.getUnpublishedAt());
        assertNull(replacement.getUnpublishedAt());
        verify(documentRepository).save(main);
    }

    // Chatbot dang khong dung (chua xac nhan / da go roi) -> khong co gi de go, 409.
    @Test
    void unpublishRejectsDocumentNotInUse() {
        Document main = row(1L, null, DocumentStatus.DONE);
        main.setContentApprovedAt(null);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(main));

        assertThrows(IllegalStateException.class, () -> documentStatusService.unpublish(1L));

        verify(documentRepository, never()).save(any());
        assertNull(main.getUnpublishedAt());
    }

    @Test
    void unpublishRejectsAlreadyUnpublishedDocument() {
        Document main = row(1L, null, DocumentStatus.DONE);
        Instant unpublishedAt = Instant.parse("2026-11-01T00:00:00Z");
        main.setUnpublishedAt(unpublishedAt);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(main));

        assertThrows(IllegalStateException.class, () -> documentStatusService.unpublish(1L));

        assertEquals(unpublishedAt, main.getUnpublishedAt());
        verify(documentRepository, never()).save(any());
    }

    // Dang lai: xoa co "da go" va chatbot dung lai dong chinh; ban thay the dang cho xac nhan
    // khong bi dong toi.
    @Test
    void publishPutsMainRowBackInUse() {
        Document main = row(1L, null, DocumentStatus.DONE);
        main.setUnpublishedAt(Instant.now());
        Document replacement = row(2L, 1L, DocumentStatus.DONE);
        when(documentRepository.findById(2L)).thenReturn(Optional.of(replacement));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(main));

        documentStatusService.publish(2L);

        assertNull(main.getUnpublishedAt());
        assertTrue(main.isInUse());
        assertFalse(replacement.isInUse());
        verify(documentRepository).save(main);
    }

    // Chua bi go thi khong co gi de dang lai, 409.
    @Test
    void publishRejectsDocumentNotUnpublished() {
        Document main = row(1L, null, DocumentStatus.DONE);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(main));

        assertThrows(IllegalStateException.class, () -> documentStatusService.publish(1L));

        verify(documentRepository, never()).save(any());
    }

    // ===== C6 - danh sach tai lieu: truy van nhe, dem chunk theo trang =====

    /** Bước xử lý đại diện cho 1 trạng thái - Document chỉ lưu bước, trạng thái suy ra từ đó. */
    private static DocumentProcessingStage stageOf(DocumentStatus status) {
        return switch (status) {
            case PENDING -> DocumentProcessingStage.PENDING;
            case PROCESSING -> DocumentProcessingStage.EXTRACTION;
            case DONE -> DocumentProcessingStage.DONE;
            case ERROR -> DocumentProcessingStage.ERROR;
        };
    }

    // Dong nhe cua danh sach (khong co full_text).
    private DocumentListRow listRow(long id, String title, DocumentStatus status, long groupId) {
        return new DocumentListRow() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public String getTitle() {
                return title;
            }

            @Override
            public String getSource() {
                return "uploads/" + id + ".pdf";
            }

            @Override
            public String getSourceUrl() {
                return null;
            }

            @Override
            public DocumentProcessingStage getProcessingStage() {
                return stageOf(status);
            }

            @Override
            public String getErrorMessage() {
                return null;
            }

            @Override
            public Instant getUploadedAt() {
                return Instant.now();
            }

            @Override
            public Instant getUpdatedAt() {
                return Instant.now();
            }

            @Override
            public Long getGroupId() {
                return groupId;
            }

            @Override
            public Instant getContentApprovedAt() {
                return Instant.now();
            }
        };
    }

    // Dong tong hop theo tai lieu (ket qua GROUP BY).
    private DocumentGroupRow groupRow(long groupId, long rowCount, Long inUseId, Instant unpublishedAt) {
        return new DocumentGroupRow() {
            @Override
            public Long getGroupId() {
                return groupId;
            }

            @Override
            public Long getRowCount() {
                return rowCount;
            }

            @Override
            public Long getInUseId() {
                return inUseId;
            }

            @Override
            public Instant getUnpublishedAt() {
                return unpublishedAt;
            }
        };
    }

    // Loc theo tu khoa KHONG DAU (logic cua Hai) van hoat dong tren danh sach nhe.
    @Test
    void listFiltersByStatusAndQuery() {
        when(documentRepository.findDisplayRows()).thenReturn(List.of(
                listRow(1L, "Hội chứng ruột kích thích IBS", DocumentStatus.DONE, 1L),
                listRow(2L, "Viêm loét dạ dày", DocumentStatus.ERROR, 2L)));
        when(documentRepository.findGroupRows()).thenReturn(List.of(
                groupRow(1L, 1, 1L, null),
                groupRow(2L, 1, null, null)));
        when(chunkRepository.countByDocumentIdIn(any()))
                .thenReturn(List.<Object[]>of(new Object[] { 1L, 15L }));

        var page = documentStatusService.list("ruot kich thich", null, 0, 10);

        assertEquals(1, page.total());
        assertEquals(1L, page.items().get(0).id());
        assertEquals(15L, page.items().get(0).chunkCount());
    }

    // Tai lieu dang co ban thay the (id 3) dang xu ly, chatbot van dung dong chinh (id 1):
    // dong hien la ban thay the, so chunk lay theo DONG CHATBOT DANG DUNG (id 1).
    @Test
    void listShowsReplacementRowButCountsChunksOfRowInUse() {
        when(documentRepository.findDisplayRows()).thenReturn(List.of(
                listRow(3L, "Phác đồ HP", DocumentStatus.PROCESSING, 1L)));
        when(documentRepository.findGroupRows()).thenReturn(List.of(
                groupRow(1L, 2, 1L, null)));
        when(chunkRepository.countByDocumentIdIn(any()))
                .thenReturn(List.<Object[]>of(new Object[] { 1L, 40L }));

        var page = documentStatusService.list(null, null, 0, 10);

        assertEquals(1, page.total());
        var item = page.items().get(0);
        assertEquals(3L, item.id());
        assertTrue(item.inUse());
        assertTrue(item.replacing());
        assertEquals(40L, item.chunkCount());
        verify(chunkRepository).countByDocumentIdIn(List.of(1L));
    }

    // Chatbot chua dung tai lieu nao -> inUse = false, chunk dem theo chinh dong dang hien.
    @Test
    void listReportsNotInUseWhenNoRowIsInUse() {
        when(documentRepository.findDisplayRows()).thenReturn(List.of(
                listRow(3L, "Phác đồ HP", DocumentStatus.DONE, 3L)));
        when(documentRepository.findGroupRows()).thenReturn(List.of(
                groupRow(3L, 1, null, null)));
        when(chunkRepository.countByDocumentIdIn(any())).thenReturn(new ArrayList<>());

        var page = documentStatusService.list(null, null, 0, 10);

        assertFalse(page.items().get(0).inUse());
        assertFalse(page.items().get(0).replacing());
        assertEquals(0L, page.items().get(0).chunkCount());
        verify(chunkRepository).countByDocumentIdIn(List.of(3L));
    }

    // UC0055 - loc theo ngay cap nhat, tinh theo gio Viet Nam, ca hai dau tinh tron ngay.
    @Test
    void listFiltersByUpdatedDate() {
        when(documentRepository.findDisplayRows()).thenReturn(List.of(
                listRow(1L, "Phác đồ HP", DocumentStatus.DONE, 1L)));
        when(documentRepository.findGroupRows()).thenReturn(List.of(
                groupRow(1L, 1, 1L, null)));
        when(chunkRepository.countByDocumentIdIn(any())).thenReturn(new ArrayList<>());
        java.time.LocalDate today = java.time.LocalDate.now(vn.gastroai.be.application.support.VietnamDateRange.ZONE);

        assertEquals(1, documentStatusService.list(null, null, today, today, 0, 10).total());
        assertEquals(0, documentStatusService.list(null, null, today.plusDays(1), null, 0, 10).total());
        assertEquals(0, documentStatusService.list(null, null, null, today.minusDays(1), 0, 10).total());
    }

    @Test
    void listRejectsFromDateAfterToDate() {
        java.time.LocalDate today = java.time.LocalDate.now();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> documentStatusService.list(null, null, today, today.minusDays(1), 0, 10));

        assertEquals("Từ ngày phải trước hoặc bằng Đến ngày", error.getMessage());
    }

    // Nhom co unpublishedAt -> unpublished = true.
    @Test
    void listFlagsUnpublishedDocument() {
        when(documentRepository.findDisplayRows()).thenReturn(List.of(
                listRow(1L, "Phác đồ HP", DocumentStatus.DONE, 1L)));
        when(documentRepository.findGroupRows()).thenReturn(List.of(
                groupRow(1L, 1, null, Instant.now())));
        when(chunkRepository.countByDocumentIdIn(any())).thenReturn(new ArrayList<>());

        var page = documentStatusService.list(null, null, 0, 10);

        assertTrue(page.items().get(0).unpublished());
        assertFalse(page.items().get(0).inUse());
    }

    // Tai lieu dang cho xu ly, chua ai go -> khong phai "da go".
    @Test
    void listDoesNotFlagWaitingDocumentAsUnpublished() {
        when(documentRepository.findDisplayRows()).thenReturn(List.of(
                listRow(1L, "Phác đồ HP", DocumentStatus.PROCESSING, 1L)));
        when(documentRepository.findGroupRows()).thenReturn(List.of(
                groupRow(1L, 1, null, null)));
        when(chunkRepository.countByDocumentIdIn(any())).thenReturn(new ArrayList<>());

        var page = documentStatusService.list(null, null, 0, 10);

        assertFalse(page.items().get(0).unpublished());
        assertFalse(page.items().get(0).inUse());
    }

    // Muc tieu chinh cua C6: chi dem chunk cho cac dong cua TRANG HIEN TAI, khong
    // dem ca bang.
    @Test
    void listCountsChunksOnlyForRowsOfCurrentPage() {
        when(documentRepository.findDisplayRows()).thenReturn(List.of(
                listRow(1L, "A", DocumentStatus.DONE, 1L),
                listRow(2L, "B", DocumentStatus.DONE, 2L),
                listRow(3L, "C", DocumentStatus.DONE, 3L)));
        when(documentRepository.findGroupRows()).thenReturn(List.of(
                groupRow(1L, 1, 1L, null),
                groupRow(2L, 1, 2L, null),
                groupRow(3L, 1, 3L, null)));
        when(chunkRepository.countByDocumentIdIn(any())).thenReturn(new ArrayList<>());

        var page = documentStatusService.list(null, null, 0, 2);

        assertEquals(3, page.total());
        assertEquals(2, page.items().size());
        verify(chunkRepository).countByDocumentIdIn(List.of(1L, 2L));
    }

    @Test
    void updateInfoUpdatesMainRowAndReplacement() {
        Document v2 = new Document();
        v2.setId(2L);
        v2.setReplacesId(1L);
        when(documentRepository.findById(2L)).thenReturn(Optional.of(v2));

        documentStatusService.updateInfo(2L, "  Trào ngược   dạ dày  ", " https://moh.gov.vn/gerd ");

        verify(documentRepository).updateGroupInfo(eq(1L), eq("Trào ngược dạ dày"),
                eq("https://moh.gov.vn/gerd"), any());
    }

    // Ten rong -> 400, khong ghi gi.
    @Test
    void updateInfoRejectsBlankTitle() {
        Document doc = new Document();
        doc.setId(1L);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

        assertThrows(IllegalArgumentException.class, () -> documentStatusService.updateInfo(1L, "   ", null));
        verify(documentRepository, never()).updateGroupInfo(any(), any(), any(), any());
    }

    // Tai lieu khong ton tai -> 404.
    @Test
    void updateInfoThrowsNotFoundForUnknownDocument() {
        when(documentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> documentStatusService.updateInfo(99L, "Tên mới", null));
    }

    // URL de trong -> ghi null (xoa URL), ten van cap nhat binh thuong.
    @Test
    void updateInfoClearsSourceUrlWhenBlank() {
        Document doc = new Document();
        doc.setId(1L);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

        documentStatusService.updateInfo(1L, "Tên mới", "   ");

        verify(documentRepository).updateGroupInfo(eq(1L), eq("Tên mới"), isNull(), any());
    }

    // URL khong phai http/https -> 400, khong ghi gi xuong DB.
    @Test
    void updateInfoRejectsInvalidSourceUrl() {
        Document doc = new Document();
        doc.setId(1L);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

        assertThrows(IllegalArgumentException.class,
                () -> documentStatusService.updateInfo(1L, "Tên mới", "javascript:alert(1)"));
        verify(documentRepository, never()).updateGroupInfo(any(), any(), any(), any());
    }

    @Test
    void listStatsCountAllDocumentsRegardlessOfFilterAndPage() {
        when(documentRepository.findDisplayRows()).thenReturn(List.of(
                listRow(1L, "Dang dung", DocumentStatus.DONE, 1L),
                listRow(2L, "Bi loi", DocumentStatus.ERROR, 2L),
                listRow(3L, "Cho xu ly", DocumentStatus.PENDING, 3L),
                listRow(4L, "Da go", DocumentStatus.DONE, 4L)));
        when(documentRepository.findGroupRows()).thenReturn(List.of(
                groupRow(1L, 1, 1L, null),
                groupRow(2L, 1, null, null),
                groupRow(3L, 1, null, null),
                groupRow(4L, 1, null, Instant.now())));
        when(chunkRepository.countByDocumentIdIn(any())).thenReturn(new ArrayList<>());

        var page = documentStatusService.list(null, DocumentStatus.DONE, 0, 1);

        // Danh sach: chi 2 tai lieu DONE, trang chi lay 1 dong.
        assertEquals(2, page.total());
        assertEquals(1, page.items().size());
        // Thong ke: van dem tren ca 4 tai lieu.
        assertEquals(4, page.stats().total());
        assertEquals(1, page.stats().ready());
        assertEquals(1, page.stats().processing());
        assertEquals(1, page.stats().error());
    }
}