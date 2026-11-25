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

    /** Chữ tiếng Việt có dấu - chữ ở cùng vị trí trong VI_UPPER (hoa) và VI_PLAIN (bỏ dấu) là cùng 1 chữ. */
    String VI_LOWER = "àáảãạăằắẳẵặâầấẩẫậèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵđ";
    String VI_UPPER = "ÀÁẢÃẠĂẰẮẲẴẶÂẦẤẨẪẬÈÉẺẼẸÊỀẾỂỄỆÌÍỈĨỊÒÓỎÕỌÔỒỐỔỖỘƠỜỚỞỠỢÙÚỦŨỤƯỪỨỬỮỰỲÝỶỸỴĐ";
    String VI_PLAIN = "aaaaaaaaaaaaaaaaaeeeeeeeeeeeiiiiiooooooooooooooooouuuuuuuuuuuyyyyyd";
    /** Xuống dòng / tab -> dấu cách, để "giảm\nđau" trong file PDF vẫn khớp khi gõ "giảm đau". */
    String LINE_BREAKS = "\n\r\t";
    String LINE_BREAKS_AS_SPACE = "   ";
    /** Dấu rời (file lưu chữ dạng tách dấu): có trong FROM mà không có trong TO nên TRANSLATE xoá đi. */
    String COMBINING_MARKS = "\u0300\u0301\u0302\u0303\u0306\u0309\u031B\u0323";

    /**
     * Gõ không dấu: so trên nội dung đã bỏ dấu + chữ thường (giống VietnameseText.normalize() ở Java).
     * Dùng TRANSLATE có sẵn của Postgres vì user ứng dụng không có quyền CREATE EXTENSION unaccent.
     */
    String FOLDED_CONTENT = "LOWER(TRANSLATE(c.content, '" + VI_LOWER + VI_UPPER + LINE_BREAKS + COMBINING_MARKS
            + "', '" + VI_PLAIN + VI_PLAIN + LINE_BREAKS_AS_SPACE + "'))";

    /**
     * Gõ có dấu: giữ dấu, chỉ đưa về chữ thường - "đau" không khớp "đầu". Tự đổi chữ hoa có dấu
     * bằng TRANSLATE vì LOWER() của Postgres chỉ chắc chắn đúng với chữ không dấu.
     */
    String LOWERED_CONTENT = "LOWER(TRANSLATE(c.content, '" + VI_UPPER + LINE_BREAKS
            + "', '" + VI_LOWER + LINE_BREAKS_AS_SPACE + "'))";

    /**
     * Chỉ tìm trong bản chatbot đang dùng. :q đã chuẩn hoá ở Java theo đúng kiểu :keepAccents chọn,
     * và đã escape bằng SearchQueryEscaper (ký tự '!').
     */
    String MATCHING_CURRENT_CHUNKS = "FROM chunks c JOIN documents d ON d.id = c.document_id "
            + "WHERE d.content_approved_at IS NOT NULL AND d.unpublished_at IS NULL "
            + "AND (CASE WHEN :keepAccents THEN " + LOWERED_CONTENT + " ELSE " + FOLDED_CONTENT + " END) "
            + "LIKE CONCAT('%', :q, '%') ESCAPE '!' ";

    int MAX_SEARCH_DOCUMENTS = 20;
    int MAX_CHUNKS_PER_DOCUMENT = 200;

    /**
     * UC0055 - mỗi tài liệu 1 dòng: số đoạn khớp, nội dung đoạn khớp đầu tiên (để cắt câu trích),
     * tổng số tài liệu khớp (COUNT OVER tính trước LIMIT). Tài liệu khớp nhiều đoạn nhất lên đầu.
     */
    @Query(value = "SELECT c.document_id AS documentId, d.title AS title, COUNT(*) AS matchCount, "
            + "(ARRAY_AGG(c.content ORDER BY c.chunk_index))[1] AS firstContent, "
            + "COUNT(*) OVER () AS totalDocuments "
            + MATCHING_CURRENT_CHUNKS
            + "GROUP BY c.document_id, d.title "
            + "ORDER BY matchCount DESC, d.title "
            + "LIMIT " + MAX_SEARCH_DOCUMENTS, nativeQuery = true)
    List<ChunkSearchRow> searchDocuments(@Param("q") String query, @Param("keepAccents") boolean keepAccents);

    /** Các đoạn khớp của 1 tài liệu - chỉ gọi khi người dùng bấm "Xem các đoạn". */
    @Query(value = "SELECT c.chunk_index AS chunkIndex, c.content AS content "
            + MATCHING_CURRENT_CHUNKS
            + "AND c.document_id = :documentId "
            + "ORDER BY c.chunk_index "
            + "LIMIT " + MAX_CHUNKS_PER_DOCUMENT, nativeQuery = true)
    List<ChunkContentRow> searchChunksInDocument(@Param("documentId") Long documentId,
                                                 @Param("q") String query,
                                                 @Param("keepAccents") boolean keepAccents);
}