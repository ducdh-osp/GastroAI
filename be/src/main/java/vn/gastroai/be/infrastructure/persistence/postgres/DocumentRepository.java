package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.domain.rag.Document;
import vn.gastroai.be.domain.rag.DocumentStatus;

import java.util.List;
import java.util.Optional;

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

        /** Xoa moi phien ban cua 1 nhom bang 1 cau lenh (chunk/embedding tu xoa theo cascade). */
        @Modifying
        @Transactional
        @Query("DELETE FROM Document d WHERE COALESCE(d.rootId, d.id) = :groupId")
        int deleteGroup(@Param("groupId") Long groupId);
}