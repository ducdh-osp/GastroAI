package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.domain.rag.Document;
import vn.gastroai.be.domain.rag.DocumentStatus;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

        List<Document> findAllByOrderByUploadedAtDesc();

        /**
         * Tim cac document dang do o nhung trang thai chua hoan tat - dung de phuc hoi
         * sau khi server khoi dong lai.
         */
        List<Document> findByStatusIn(List<DocumentStatus> statuses);

        List<Document> findByContentHashAndStatusNot(String contentHash, DocumentStatus status);

        /**
         * Gianh quyen xu ly 1 document bang UPDATE nguyen tu - chi doi duoc neu con
         * dang
         * PENDING. Tra ve so dong bi doi: 1 = gianh thanh cong, 0 = da bi luot quet
         * khac
         * (hoac node khac sau nay) gianh mat truoc, bo qua.
         */
        @Modifying
        @Transactional
        @Query("UPDATE Document d SET d.status = vn.gastroai.be.domain.rag.DocumentStatus.PROCESSING, "
                        + "d.updatedAt = CURRENT_TIMESTAMP "
                        + "WHERE d.id = :id AND d.status = vn.gastroai.be.domain.rag.DocumentStatus.PENDING")
        int claimForProcessing(@Param("id") Long id);

        /**
         * Tra 1 document da gianh duoc ve lai PENDING - dung khi dispatch that bai (vd
         * pool day cho).
         */
        @Modifying
        @Transactional
        @Query("UPDATE Document d SET d.status = vn.gastroai.be.domain.rag.DocumentStatus.PENDING, "
                        + "d.updatedAt = CURRENT_TIMESTAMP "
                        + "WHERE d.id = :id")
        int revertToPending(@Param("id") Long id);

        /**
         * Doi hang loat document con dang PROCESSING ve PENDING - dung dung 1 lan luc
         * server khoi dong, vi luc nay chac chan khong con worker nao that su dang
         * chay.
         */
        @Modifying
        @Transactional
        @Query("UPDATE Document d SET d.status = vn.gastroai.be.domain.rag.DocumentStatus.PENDING, "
                        + "d.updatedAt = CURRENT_TIMESTAMP "
                        + "WHERE d.status = vn.gastroai.be.domain.rag.DocumentStatus.PROCESSING")
        int requeueStuckProcessing();

        @Query("SELECT d FROM Document d WHERE COALESCE(d.rootId, d.id) = :groupId ORDER BY d.versionNo DESC")
        List<Document> findGroup(@Param("groupId") Long groupId);

        /**
         * Tat "dang dung" cho ca nhom. Phai goi TRUOC khi bat ban moi (xem
         * uk_documents_group_current).
         */
        @Modifying
        @Transactional
        @Query("UPDATE Document d SET d.currentVersion = false "
                        + "WHERE COALESCE(d.rootId, d.id) = :groupId AND d.currentVersion = true")
        int clearCurrent(@Param("groupId") Long groupId);

        /**
         * Xoa moi phien ban cua 1 nhom bang 1 cau lenh (chunk/embedding tu xoa theo
         * cascade).
         */
        @Modifying
        @Transactional
        @Query("DELETE FROM Document d WHERE COALESCE(d.rootId, d.id) = :groupId")
        int deleteGroup(@Param("groupId") Long groupId);

        /**
         * Moi tai lieu 1 dong: phien ban co versionNo lon nhat trong nhom. KHONG lay
         * full_text
         * (cot nay chua toan bo chu cua tai lieu, rat nang) - chi cac cot hien len bang
         * danh sach.
         */
        @Query("SELECT d.id AS id, d.title AS title, d.source AS source, d.sourceUrl AS sourceUrl, "
                        + "d.status AS status, d.processingStage AS processingStage, "
                        + "d.errorMessage AS errorMessage, d.uploadedAt AS uploadedAt, "
                        + "d.updatedAt AS updatedAt, d.versionNo AS versionNo, "
                        + "COALESCE(d.rootId, d.id) AS groupId "
                        + "FROM Document d "
                        + "WHERE d.versionNo = (SELECT MAX(x.versionNo) FROM Document x "
                        + "WHERE COALESCE(x.rootId, x.id) = COALESCE(d.rootId, d.id)) "
                        + "ORDER BY d.uploadedAt DESC")
        List<DocumentListRow> findLatestVersionRows();

        /**
         * Thong tin tong hop theo nhom (moi tai lieu 1 dong): so phien ban, ban dang
         * dung,
         * thoi diem go (chi dong goc moi co unpublished_at).
         */
        @Query("SELECT COALESCE(d.rootId, d.id) AS groupId, COUNT(d) AS versionCount, "
                        + "MAX(CASE WHEN d.currentVersion = TRUE THEN d.versionNo END) AS currentVersionNo, "
                        + "MAX(CASE WHEN d.currentVersion = TRUE THEN d.id END) AS currentId, "
                        + "MAX(CASE WHEN d.rootId IS NULL THEN d.unpublishedAt END) AS unpublishedAt "
                        + "FROM Document d GROUP BY COALESCE(d.rootId, d.id)")
        List<DocumentGroupRow> findGroupRows();
}