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

        /**
         * Moi dong cua 1 tai lieu: dong chinh + ban thay the (neu co). Ban thay the luon moi hon
         * nen id lon hon - xep id giam dan thi ban thay the dung dau.
         */
        @Query("SELECT d FROM Document d WHERE COALESCE(d.replacesId, d.id) = :groupId ORDER BY d.id DESC")
        List<Document> findGroup(@Param("groupId") Long groupId);

        /**
         * Moi tai lieu 1 dong de hien danh sach: ban thay the neu dang co, khong thi dong chinh.
         * KHONG lay full_text (cot nay chua toan bo chu cua tai lieu, rat nang).
         */
        @Query("SELECT d.id AS id, d.title AS title, d.source AS source, d.sourceUrl AS sourceUrl, "
                        + "d.status AS status, d.processingStage AS processingStage, "
                        + "d.errorMessage AS errorMessage, d.uploadedAt AS uploadedAt, "
                        + "d.updatedAt AS updatedAt, "
                        + "d.contentApprovedAt AS contentApprovedAt, "
                        + "COALESCE(d.replacesId, d.id) AS groupId "
                        + "FROM Document d "
                        + "WHERE d.replacesId IS NOT NULL "
                        + "OR NOT EXISTS (SELECT 1 FROM Document x WHERE x.replacesId = d.id) "
                        + "ORDER BY d.uploadedAt DESC")
        List<DocumentListRow> findDisplayRows();

        /**
         * Thong tin tong hop theo tai lieu: so dong (2 = dang co ban thay the), dong chatbot dang
         * dung, thoi diem go (chi dong chinh moi co unpublished_at).
         */
        @Query("SELECT COALESCE(d.replacesId, d.id) AS groupId, COUNT(d) AS rowCount, "
                        + "MAX(CASE WHEN d.contentApprovedAt IS NOT NULL AND d.unpublishedAt IS NULL "
                        + "THEN d.id END) AS inUseId, "
                        + "MAX(CASE WHEN d.replacesId IS NULL THEN d.unpublishedAt END) AS unpublishedAt "
                        + "FROM Document d GROUP BY COALESCE(d.replacesId, d.id)")
        List<DocumentGroupRow> findGroupRows();

        @Modifying
        @Transactional
        @Query("UPDATE Document d SET d.title = :title, d.sourceUrl = :sourceUrl, d.updatedAt = :now "
                        + "WHERE COALESCE(d.replacesId, d.id) = :groupId")
        int updateGroupInfo(@Param("groupId") Long groupId,
                        @Param("title") String title,
                        @Param("sourceUrl") String sourceUrl,
                        @Param("now") java.time.Instant now);
}