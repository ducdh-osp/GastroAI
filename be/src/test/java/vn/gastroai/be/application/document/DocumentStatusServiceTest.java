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
import vn.gastroai.be.infrastructure.persistence.postgres.ChunkSearchRow;
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
        return document;
    }

    // Ban 2 xu ly xong nhung chua la ban hien hanh -> markDone phai chuyen "hien
    // hanh" sang ban 2.
    // clearCurrent PHAI chay truoc setCurrentVersion/save, neu khong index
    // uk_documents_group_current bi vi pham (2 ban cung la hien hanh trong 1 nhom).
    @Test
    void markDoneSwitchesCurrentVersionForNonCurrentDocument() {
        Document root = version(1L, null, 1, false, DocumentStatus.DONE);
        Document v2 = version(2L, 1L, 2, false, DocumentStatus.PROCESSING);
        when(documentRepository.findById(2L)).thenReturn(Optional.of(v2));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(root));

        documentStatusService.markDone(2L);

        assertEquals(DocumentStatus.DONE, v2.getStatus());
        assertTrue(v2.isCurrentVersion());

        InOrder order = inOrder(documentRepository);
        order.verify(documentRepository).clearCurrent(1L);
        order.verify(documentRepository).save(v2);
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
    void searchContentRejectsShortQuery() {
        assertThrows(IllegalArgumentException.class,
                () -> documentStatusService.searchContent(" a "));

        verify(chunkRepository, never()).searchContent(any());
    }

    // % va _ la ky tu dai dien cua LIKE; neu khong escape thi "50%_x" khop gan nhu
    // moi thu.
    // Trong Java, chuoi "50\\%\\_x" chinh la 50\%\_x.
    @Test
    void searchContentEscapesLikeWildcards() {
        when(chunkRepository.searchContent(any())).thenReturn(List.of());

        documentStatusService.searchContent("50%_x");

        verify(chunkRepository).searchContent("50\\%\\_x");
    }

    // Doan trich bi cat o ca 2 dau -> co dau "…" o dau va cuoi; chu hoa van duoc
    // giu vi chi
    // dung chu thuong de tim vi tri.
    @Test
    void snippetAddsEllipsisWhenCut() {
        String content = "a".repeat(300) + " Helicobacter " + "b".repeat(300);

        String result = DocumentStatusService.snippet(content, "helicobacter");

        assertTrue(result.startsWith("…"));
        assertTrue(result.endsWith("…"));
        assertTrue(result.contains("Helicobacter"));
    }

    // Noi dung ngan, khong can cat -> giu nguyen, khong them "…".
    @Test
    void snippetWithoutCutHasNoEllipsis() {
        String result = DocumentStatusService.snippet("Điều trị HP", "HP");

        assertEquals("Điều trị HP", result);
    }

    // ===== C1 - go co chu dich + chi ban DONE moi duoc dung =====

    // Tai lieu moi: v1 xu ly xong, chua ai go -> v1 tro thanh ban dang dung.
    @Test
    void markDoneActivatesFirstVersionOfNewDocument() {
        Document v1 = version(1L, null, 1, false, DocumentStatus.PROCESSING);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));

        documentStatusService.markDone(1L);

        assertTrue(v1.isCurrentVersion());
        InOrder order = inOrder(documentRepository);
        order.verify(documentRepository).clearCurrent(1L);
        order.verify(documentRepository).save(v1);
    }

    // Admin da go tai lieu -> tai ban moi xong xuoi (DONE) cung KHONG tu dong thanh
    // ban dang dung.
    @Test
    void markDoneDoesNotActivateWhenRootIsUnpublished() {
        Document root = version(1L, null, 1, false, DocumentStatus.DONE);
        root.setUnpublishedAt(Instant.now());
        Document v2 = version(2L, 1L, 2, false, DocumentStatus.PROCESSING);
        when(documentRepository.findById(2L)).thenReturn(Optional.of(v2));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(root));

        documentStatusService.markDone(2L);

        assertEquals(DocumentStatus.DONE, v2.getStatus());
        assertFalse(v2.isCurrentVersion());
        verify(documentRepository, never()).clearCurrent(any());
        verify(documentRepository).save(v2);
    }

    // Go: dong goc duoc danh dau unpublishedAt, co "hien hanh" cua dong goc tat
    // trong bo nho
    // (tranh save(root) ghi de is_current = true tro lai).
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
    
    // ===== C4 - tim kiem: bao bi cat + gioi han do dai =====

    // Dong ket qua gia: chi can chunkIndex khac nhau, cac truong khac co dinh.
    private ChunkSearchRow searchRow(int chunkIndex) {
        return new ChunkSearchRow() {
            @Override
            public Long getDocumentId() {
                return 1L;
            }

            @Override
            public String getTitle() {
                return "Phac do HP";
            }

            @Override
            public Integer getChunkIndex() {
                return chunkIndex;
            }

            @Override
            public String getContent() {
                return "Noi dung HP doan " + chunkIndex;
            }
        };
    }

    private List<ChunkSearchRow> searchRows(int count) {
        List<ChunkSearchRow> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            rows.add(searchRow(i));
        }
        return rows;
    }

    // Tu khoa 201 ky tu -> 400 (IllegalArgumentException), khong cham toi DB.
    @Test
    void searchContentRejectsTooLongQuery() {
        String tooLong = "a".repeat(201);

        assertThrows(IllegalArgumentException.class,
                () -> documentStatusService.searchContent(tooLong));

        verify(chunkRepository, never()).searchContent(any());
    }

    // Dung 200 ky tu van duoc chap nhan (ranh gioi).
    @Test
    void searchContentAcceptsQueryOfMaxLength() {
        when(chunkRepository.searchContent(any())).thenReturn(List.of());

        var result = documentStatusService.searchContent("a".repeat(200));

        assertTrue(result.items().isEmpty());
        assertFalse(result.truncated());
    }

    // Repository tra 51 dong (dong thu 51 chi de biet con ket qua) -> chi giu 50, truncated = true.
    @Test
    void searchContentMarksTruncatedWhenMoreThan50Rows() {
        when(chunkRepository.searchContent(any())).thenReturn(searchRows(51));

        var result = documentStatusService.searchContent("hp");

        assertEquals(50, result.items().size());
        assertEquals(49, result.items().get(49).chunkIndex());
        assertTrue(result.truncated());
    }

    // Dung 50 dong -> khong co gi bi cat, khong duoc bao "con nua".
    @Test
    void searchContentNotTruncatedWhenExactly50Rows() {
        when(chunkRepository.searchContent(any())).thenReturn(searchRows(50));

        var result = documentStatusService.searchContent("hp");

        assertEquals(50, result.items().size());
        assertFalse(result.truncated());
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

    // Khong ban nao dang dung -> currentVersionNo la null, chunk dem theo dong dang hien.
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

    // Muc tieu chinh cua C6: chi dem chunk cho cac dong cua TRANG HIEN TAI, khong dem ca bang.
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
}