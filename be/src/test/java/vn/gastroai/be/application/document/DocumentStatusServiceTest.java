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
                () -> documentStatusService.retry(42L));
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
        // Ban DONE trong cac test cu coi nhu Bien tap da xac nhan; test can ban "cho xac nhan" tu dat null.
        if (status == DocumentStatus.DONE) {
            document.setContentApprovedAt(Instant.now());
        }
        return document;
    }

    // Ban 1 (hoac ban da la hien hanh) xong -> khong dong vao co hien hanh cua ai
    // ca.
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

    // ===== SCR-AD-04 §3.1b - Bien tap xac nhan noi dung truoc khi chatbot dung =====

    // Xu ly ky thuat xong chua duoc dung ngay: van cho xac nhan, khong dong vao ban dang dung.
    @Test
    void markDoneWaitsForContentApprovalBeforeAiUsesIt() {
        Document v2 = version(2L, 1L, 2, false, DocumentStatus.PROCESSING);
        when(documentRepository.findById(2L)).thenReturn(Optional.of(v2));

        documentStatusService.markDone(2L);

        assertEquals(DocumentStatus.DONE, v2.getStatus());
        assertFalse(v2.isCurrentVersion());
        assertNull(v2.getContentApprovedAt());
        verify(documentRepository, never()).clearCurrent(any());
        verify(documentRepository).save(v2);
    }

    // Xac nhan ban moi hon ban dang dung -> ban nay thanh ban dang dung, luu ai xac nhan.
    // clearCurrent phai chay truoc save (index uk_documents_group_current).
    @Test
    void approveContentMakesNewerVersionCurrentAndRecordsApprover() {
        Document v1 = version(1L, null, 1, true, DocumentStatus.DONE);
        Document v2 = version(2L, 1L, 2, false, DocumentStatus.DONE);
        v2.setContentApprovedAt(null);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));
        when(documentRepository.findById(2L)).thenReturn(Optional.of(v2));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(v2, v1));

        documentStatusService.approveContent(1L, 2L, "bientap@gastroai.vn");

        assertNotNull(v2.getContentApprovedAt());
        assertEquals("bientap@gastroai.vn", v2.getContentApprovedBy());
        assertTrue(v2.isCurrentVersion());
        InOrder order = inOrder(documentRepository);
        order.verify(documentRepository).clearCurrent(1L);
        order.verify(documentRepository).save(v2);
    }

    // Tai lieu da bi go -> xac nhan xong van khong tu dua len chatbot.
    @Test
    void approveContentKeepsUnpublishedDocumentOffline() {
        Document root = version(1L, null, 1, false, DocumentStatus.DONE);
        root.setUnpublishedAt(Instant.now());
        Document v2 = version(2L, 1L, 2, false, DocumentStatus.DONE);
        v2.setContentApprovedAt(null);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(root));
        when(documentRepository.findById(2L)).thenReturn(Optional.of(v2));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(v2, root));

        documentStatusService.approveContent(1L, 2L, "bientap@gastroai.vn");

        assertNotNull(v2.getContentApprovedAt());
        assertFalse(v2.isCurrentVersion());
        verify(documentRepository, never()).clearCurrent(any());
    }

    // Xac nhan 1 ban cu hon ban dang dung -> khong keo chatbot lui ve ban cu.
    @Test
    void approveContentDoesNotReplaceNewerCurrentVersion() {
        Document v1 = version(1L, null, 1, false, DocumentStatus.DONE);
        Document v2 = version(2L, 1L, 2, false, DocumentStatus.DONE);
        v2.setContentApprovedAt(null);
        Document v3 = version(3L, 1L, 3, true, DocumentStatus.DONE);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));
        when(documentRepository.findById(2L)).thenReturn(Optional.of(v2));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(v3, v2, v1));

        documentStatusService.approveContent(1L, 2L, "bientap@gastroai.vn");

        assertNotNull(v2.getContentApprovedAt());
        assertFalse(v2.isCurrentVersion());
        assertTrue(v3.isCurrentVersion());
        verify(documentRepository, never()).clearCurrent(any());
    }

    @Test
    void approveContentRejectsAlreadyApprovedVersion() {
        Document v1 = version(1L, null, 1, true, DocumentStatus.DONE);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));

        assertThrows(IllegalStateException.class,
                () -> documentStatusService.approveContent(1L, 1L, "bientap@gastroai.vn"));

        verify(documentRepository, never()).save(any());
    }

    // Chua xac nhan noi dung thi Admin cung khong bat len cho chatbot dung duoc.
    @Test
    void activateVersionRejectsUnapprovedVersion() {
        Document v1 = version(1L, null, 1, true, DocumentStatus.DONE);
        Document v2 = version(2L, 1L, 2, false, DocumentStatus.DONE);
        v2.setContentApprovedAt(null);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));
        when(documentRepository.findById(2L)).thenReturn(Optional.of(v2));

        assertThrows(IllegalStateException.class, () -> documentStatusService.activateVersion(1L, 2L));

        verify(documentRepository, never()).clearCurrent(any());
    }

    // Dang lai: bo qua ban moi nhat dang cho xac nhan, bat ban da xac nhan gan nhat.
    @Test
    void publishSkipsVersionWaitingForApproval() {
        Document v1 = version(1L, null, 1, false, DocumentStatus.DONE);
        Document v2 = version(2L, 1L, 2, false, DocumentStatus.DONE);
        v2.setContentApprovedAt(null);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(v2, v1));

        documentStatusService.publish(1L);

        assertTrue(v1.isCurrentVersion());
        assertFalse(v2.isCurrentVersion());
    }

    // Phien ban thuoc nhom khac (rootId khac) -> khong duoc kich hoat, coi nhu
    // khong tim thay.
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

    // Truong hop thanh cong: bo co hien hanh cua ca nhom TRUOC, bat ban dich SAU,
    // roi save.
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

    @Test
    void unpublishClearsCurrentForGroup() {
        Document root = version(1L, null, 1, false, DocumentStatus.DONE);
        Document v2 = version(2L, 1L, 2, true, DocumentStatus.DONE);
        when(documentRepository.findById(2L)).thenReturn(Optional.of(v2));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(root));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(v2, root));
        documentStatusService.unpublish(2L);

        verify(documentRepository).clearCurrent(1L);
    }

    // Nhom khong co ban DONE nao (vd tat ca ERROR) -> khong co gi de dang, 409.
    @Test
    void publishRejectsWhenNoDoneVersion() {
        Document v1 = version(1L, null, 1, false, DocumentStatus.ERROR);
        Document v2 = version(2L, 1L, 2, false, DocumentStatus.ERROR);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(v2, v1));

        assertThrows(IllegalStateException.class, () -> documentStatusService.publish(1L));

        verify(documentRepository, never()).clearCurrent(any());
        verify(documentRepository, never()).save(any());
    }

    // Ban moi nhat (v3) bi ERROR -> dang ban DONE gan nhat (v2), khong phai v3,
    // cung khong phai v1.
    // clearCurrent phai chay truoc save (index uk_documents_group_current).
    @Test
    void publishActivatesLatestDoneVersion() {
        Document v1 = version(1L, null, 1, false, DocumentStatus.DONE);
        Document v2 = version(2L, 1L, 2, false, DocumentStatus.DONE);
        Document v3 = version(3L, 1L, 3, false, DocumentStatus.ERROR);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(v3, v2, v1));

        documentStatusService.publish(1L);

        assertTrue(v2.isCurrentVersion());
        assertFalse(v1.isCurrentVersion());
        assertFalse(v3.isCurrentVersion());

        InOrder order = inOrder(documentRepository);
        order.verify(documentRepository).clearCurrent(1L);
        order.verify(documentRepository).save(v2);
    }

    @Test
    void unpublishMarksRootAsUnpublished() {
        Document root = version(1L, null, 1, true, DocumentStatus.DONE);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(root));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(root));

        documentStatusService.unpublish(1L);

        assertTrue(root.getUnpublishedAt() != null);
        assertFalse(root.isCurrentVersion());
        verify(documentRepository).clearCurrent(1L);
        verify(documentRepository).save(root);
    }

    // Dang lai: xoa co go tren dong goc.
    @Test
    void publishClearsUnpublishedFlag() {
        Document root = version(1L, null, 1, false, DocumentStatus.DONE);
        root.setUnpublishedAt(Instant.now());
        when(documentRepository.findById(1L)).thenReturn(Optional.of(root));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(root));

        documentStatusService.publish(1L);

        assertNull(root.getUnpublishedAt());
        assertTrue(root.isCurrentVersion());
    }

    // Chon "Dung ban nay" cung la dang lai -> xoa co go.
    @Test
    void activateVersionClearsUnpublishedFlag() {
        Document root = version(1L, null, 1, false, DocumentStatus.DONE);
        root.setUnpublishedAt(Instant.now());
        Document v2 = version(2L, 1L, 2, false, DocumentStatus.DONE);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(root));
        when(documentRepository.findById(2L)).thenReturn(Optional.of(v2));

        documentStatusService.activateVersion(1L, 2L);

        assertNull(root.getUnpublishedAt());
        assertTrue(v2.isCurrentVersion());
    }

    @Test
    void unpublishRejectsGroupWithoutDoneVersion() {
        Document v1 = version(1L, null, 1, false, DocumentStatus.PENDING);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(v1));

        assertThrows(IllegalStateException.class, () -> documentStatusService.unpublish(1L));

        verify(documentRepository, never()).clearCurrent(any());
        verify(documentRepository, never()).save(any());
        assertNull(v1.getUnpublishedAt());
    }

    // ===== C6 - danh sach tai lieu: truy van nhe, dem chunk theo trang =====

    // Dong nhe cua danh sach (khong co full_text).
    private DocumentListRow listRow(long id, String title, DocumentStatus status, int versionNo, long groupId) {
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
            public DocumentStatus getStatus() {
                return status;
            }

            @Override
            public DocumentProcessingStage getProcessingStage() {
                return DocumentProcessingStage.DONE;
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
            public Integer getVersionNo() {
                return versionNo;
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

    // Dong tong hop theo nhom (ket qua GROUP BY).
    private DocumentGroupRow groupRow(long groupId, long versionCount, Integer currentVersionNo,
            Long currentId, Instant unpublishedAt) {
        return new DocumentGroupRow() {
            @Override
            public Long getGroupId() {
                return groupId;
            }

            @Override
            public Long getVersionCount() {
                return versionCount;
            }

            @Override
            public Integer getCurrentVersionNo() {
                return currentVersionNo;
            }

            @Override
            public Long getCurrentId() {
                return currentId;
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
        when(documentRepository.findLatestVersionRows()).thenReturn(List.of(
                listRow(1L, "Hội chứng ruột kích thích IBS", DocumentStatus.DONE, 1, 1L),
                listRow(2L, "Viêm loét dạ dày", DocumentStatus.ERROR, 1, 2L)));
        when(documentRepository.findGroupRows()).thenReturn(List.of(
                groupRow(1L, 1, 1, 1L, null),
                groupRow(2L, 1, null, null, null)));
        when(chunkRepository.countByDocumentIdIn(any()))
                .thenReturn(List.<Object[]>of(new Object[] { 1L, 15L }));

        var page = documentStatusService.list("ruot kich thich", null, 0, 10);

        assertEquals(1, page.total());
        assertEquals(1L, page.items().get(0).id());
        assertEquals(15L, page.items().get(0).chunkCount());
    }

    // Tai lieu co 3 ban: dong hien la ban moi nhat (v3), ban dang dung la v2,
    // so chunk lay theo BAN DANG DUNG (id 2), khong phai v3.
    @Test
    void listShowsOneRowPerDocumentWithLatestVersion() {
        when(documentRepository.findLatestVersionRows()).thenReturn(List.of(
                listRow(3L, "Phác đồ HP", DocumentStatus.PROCESSING, 3, 1L)));
        when(documentRepository.findGroupRows()).thenReturn(List.of(
                groupRow(1L, 3, 2, 2L, null)));
        when(chunkRepository.countByDocumentIdIn(any()))
                .thenReturn(List.<Object[]>of(new Object[] { 2L, 40L }));

        var page = documentStatusService.list(null, null, 0, 10);

        assertEquals(1, page.total());
        var item = page.items().get(0);
        assertEquals(3L, item.id());
        assertEquals(3, item.versionNo());
        assertEquals(2, item.currentVersionNo());
        assertEquals(3, item.versionCount());
        assertEquals(40L, item.chunkCount());
        verify(chunkRepository).countByDocumentIdIn(List.of(2L));
    }

    // Khong ban nao dang dung -> currentVersionNo la null, chunk dem theo dong dang
    // hien.
    @Test
    void listReturnsNullCurrentVersionNoWhenNoVersionIsCurrent() {
        when(documentRepository.findLatestVersionRows()).thenReturn(List.of(
                listRow(3L, "Phác đồ HP", DocumentStatus.DONE, 3, 1L)));
        when(documentRepository.findGroupRows()).thenReturn(List.of(
                groupRow(1L, 3, null, null, null)));
        when(chunkRepository.countByDocumentIdIn(any())).thenReturn(new ArrayList<>());

        var page = documentStatusService.list(null, null, 0, 10);

        assertNull(page.items().get(0).currentVersionNo());
        assertEquals(3, page.items().get(0).versionCount());
        assertEquals(0L, page.items().get(0).chunkCount());
        verify(chunkRepository).countByDocumentIdIn(List.of(3L));
    }

    // UC0055 - loc theo ngay cap nhat, tinh theo gio Viet Nam, ca hai dau tinh tron ngay.
    @Test
    void listFiltersByUpdatedDate() {
        when(documentRepository.findLatestVersionRows()).thenReturn(List.of(
                listRow(1L, "Phác đồ HP", DocumentStatus.DONE, 1, 1L)));
        when(documentRepository.findGroupRows()).thenReturn(List.of(
                groupRow(1L, 1, 1, 1L, null)));
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
        when(documentRepository.findLatestVersionRows()).thenReturn(List.of(
                listRow(1L, "Phác đồ HP", DocumentStatus.DONE, 1, 1L)));
        when(documentRepository.findGroupRows()).thenReturn(List.of(
                groupRow(1L, 1, null, null, Instant.now())));
        when(chunkRepository.countByDocumentIdIn(any())).thenReturn(new ArrayList<>());

        var page = documentStatusService.list(null, null, 0, 10);

        assertTrue(page.items().get(0).unpublished());
        assertNull(page.items().get(0).currentVersionNo());
    }

    // Tai lieu dang cho xu ly, chua ai go -> khong phai "da go".
    @Test
    void listDoesNotFlagWaitingDocumentAsUnpublished() {
        when(documentRepository.findLatestVersionRows()).thenReturn(List.of(
                listRow(1L, "Phác đồ HP", DocumentStatus.PROCESSING, 1, 1L)));
        when(documentRepository.findGroupRows()).thenReturn(List.of(
                groupRow(1L, 1, null, null, null)));
        when(chunkRepository.countByDocumentIdIn(any())).thenReturn(new ArrayList<>());

        var page = documentStatusService.list(null, null, 0, 10);

        assertFalse(page.items().get(0).unpublished());
        assertNull(page.items().get(0).currentVersionNo());
    }

    // Muc tieu chinh cua C6: chi dem chunk cho cac dong cua TRANG HIEN TAI, khong
    // dem ca bang.
    @Test
    void listCountsChunksOnlyForRowsOfCurrentPage() {
        when(documentRepository.findLatestVersionRows()).thenReturn(List.of(
                listRow(1L, "A", DocumentStatus.DONE, 1, 1L),
                listRow(2L, "B", DocumentStatus.DONE, 1, 2L),
                listRow(3L, "C", DocumentStatus.DONE, 1, 3L)));
        when(documentRepository.findGroupRows()).thenReturn(List.of(
                groupRow(1L, 1, 1, 1L, null),
                groupRow(2L, 1, 1, 2L, null),
                groupRow(3L, 1, 1, 3L, null)));
        when(chunkRepository.countByDocumentIdIn(any())).thenReturn(new ArrayList<>());

        var page = documentStatusService.list(null, null, 0, 2);

        assertEquals(3, page.total());
        assertEquals(2, page.items().size());
        verify(chunkRepository).countByDocumentIdIn(List.of(1L, 2L));
    }

    // ===== C8 - cac truong hop "khong lam gi" =====

    // Kich hoat dung ban dang dung -> khong dong cham vao co hien hanh cua ai ca.
    @Test
    void activateVersionDoesNothingWhenAlreadyCurrent() {
        Document v1 = version(1L, null, 1, true, DocumentStatus.DONE);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));

        documentStatusService.activateVersion(1L, 1L);

        assertTrue(v1.isCurrentVersion());
        verify(documentRepository, never()).clearCurrent(any());
        verify(documentRepository, never()).save(any());
    }

    // Ban DONE moi nhat da la ban dang dung -> dang lai khong lam gi.
    @Test
    void publishDoesNothingWhenLatestDoneVersionAlreadyCurrent() {
        Document v1 = version(1L, null, 1, false, DocumentStatus.DONE);
        Document v2 = version(2L, 1L, 2, true, DocumentStatus.DONE);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(v2, v1));

        documentStatusService.publish(1L);

        assertTrue(v2.isCurrentVersion());
        verify(documentRepository, never()).clearCurrent(any());
        verify(documentRepository, never()).save(any());
    }

    @Test
    void updateInfoUpdatesWholeVersionGroup() {
        Document v2 = new Document();
        v2.setId(2L);
        v2.setRootId(1L);
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
        when(documentRepository.findLatestVersionRows()).thenReturn(List.of(
                listRow(1L, "Dang dung", DocumentStatus.DONE, 1, 1L),
                listRow(2L, "Bi loi", DocumentStatus.ERROR, 1, 2L),
                listRow(3L, "Cho xu ly", DocumentStatus.PENDING, 1, 3L),
                listRow(4L, "Da go", DocumentStatus.DONE, 1, 4L)));
        when(documentRepository.findGroupRows()).thenReturn(List.of(
                groupRow(1L, 1, 1, 1L, null),
                groupRow(2L, 1, null, null, null),
                groupRow(3L, 1, null, null, null),
                groupRow(4L, 1, null, null, Instant.now())));
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