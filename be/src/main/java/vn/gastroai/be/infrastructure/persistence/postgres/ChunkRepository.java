package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.rag.Chunk;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;

public interface ChunkRepository extends JpaRepository<Chunk, Long> {

    List<Chunk> findByDocumentIdOrderByChunkIndex(Long documentId);

    long countByDocumentId(Long documentId);

    
    /** Dem chunk chi cho cac tai lieu duoc chon (vd cac dong cua trang hien tai), khong dem ca bang. */
    @Query("SELECT c.document.id, COUNT(c) FROM Chunk c WHERE c.document.id IN :ids GROUP BY c.document.id")
    List<Object[]> countByDocumentIdIn(@Param("ids") Collection<Long> ids);
    
    @org.springframework.data.jpa.repository.Query("SELECT c.document.id, count(c) FROM Chunk c GROUP BY c.document.id")
    List<Object[]> countByDocumentIdGrouped();

    void deleteByDocumentId(Long documentId);

    /**
     * UC0055 - tim cum tu trong noi dung chunk cua ban dang dung, toi da 50 ket
     * qua.
     */
    @Query(value = "SELECT c.document_id AS documentId, d.title AS title, "
            + "c.chunk_index AS chunkIndex, c.content AS content "
            + "FROM chunks c JOIN documents d ON d.id = c.document_id "
            + "WHERE d.status = 'DONE' AND d.is_current = TRUE "
            + "AND c.content ILIKE CONCAT('%', :q, '%') ESCAPE '\\' "
            + "ORDER BY d.title, c.chunk_index "
            + "LIMIT 51", nativeQuery = true)
    List<ChunkSearchRow> searchContent(@Param("q") String q);
}