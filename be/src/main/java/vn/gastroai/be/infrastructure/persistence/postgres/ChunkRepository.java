package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.rag.Chunk;

import java.util.List;

public interface ChunkRepository extends JpaRepository<Chunk, Long> {

    List<Chunk> findByDocumentIdOrderByChunkIndex(Long documentId);

    long countByDocumentId(Long documentId);

    @org.springframework.data.jpa.repository.Query("SELECT c.document.id, count(c) FROM Chunk c GROUP BY c.document.id")
    List<Object[]> countByDocumentIdGrouped();

    void deleteByDocumentId(Long documentId);
}