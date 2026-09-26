package vn.gastroai.be.infrastructure.rag;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;

/**
 * UC0033 (lưu embedding) / UC0028 (truy xuất theo độ tương đồng) - thao tác trực tiếp lên cột
 * kiểu "vector" (pgvector) bằng JDBC thuần thay vì qua JPA/Hibernate, vì Hibernate không có
 * type mapping sẵn cho "vector" nếu không thêm driver phụ (pgvector-java) — cân nhắc thêm
 * dependency đó sau nếu code JDBC tay ở đây trở nên cồng kềnh.
 *
 * Tự tạo JdbcTemplate riêng (thay vì để Spring Boot auto-configure) để CHẮC CHẮN luôn trỏ đúng
 * datasource Postgres, không phụ thuộc vào việc @Primary trên postgresDataSource có được
 * Spring chọn đúng hay không khi có 2 DataSource cùng tồn tại (Postgres + MySQL).
 */
@Repository
public class EmbeddingStore {
    private final JdbcTemplate jdbcTemplate;

    public EmbeddingStore(@Qualifier("postgresDataSource") DataSource postgresDataSource) {
        this.jdbcTemplate = new JdbcTemplate(postgresDataSource);
    }

    /** Lưu (hoặc ghi đè nếu chunk đã có embedding) vector cho 1 chunk. */
    @Transactional("postgresTransactionManager")
    public void save(Long chunkId, float[] embedding, String model) {
        String vectorLiteral = toVectorLiteral(embedding);
        jdbcTemplate.update(
                """
                INSERT INTO embeddings (chunk_id, embedding, model)
                VALUES (?, ?::vector, ?)
                ON CONFLICT (chunk_id) DO UPDATE
                    SET embedding = EXCLUDED.embedding, model = EXCLUDED.model
                """,
                chunkId, vectorLiteral, model);
    }

    /**
     * UC0028 - top-k chunk gần nhất theo cosine distance ("&lt;=&gt;"). Chưa có index ANN trên
     * cột embedding (xem migration V7) nên đây là sequential scan — đủ nhanh ở quy mô đồ án,
     * cần cân nhắc lại nếu kho tri thức lớn dần lên hàng chục nghìn chunk trở lên.
     *
     * UC0029 - JOIN thêm documents để lấy title, dùng hiển thị "Nguồn tham khảo" phía FE
     * (chunks.document_id NOT NULL + có FK tới documents nên INNER JOIN an toàn, xem migration V7).
     */
    public List<SimilarChunk> findTopK(float[] queryEmbedding, int topK) {
        String vectorLiteral = toVectorLiteral(queryEmbedding);
        return jdbcTemplate.query(
                """
                SELECT c.id AS chunk_id, c.document_id, d.title AS document_title, c.content,
                       e.embedding <=> ?::vector AS distance
                FROM embeddings e
                JOIN chunks c ON c.id = e.chunk_id
                JOIN documents d ON d.id = c.document_id
                ORDER BY e.embedding <=> ?::vector
                LIMIT ?
                """,
                (rs, rowNum) -> new SimilarChunk(
                        rs.getLong("chunk_id"),
                        rs.getLong("document_id"),
                        rs.getString("document_title"),
                        rs.getString("content"),
                        rs.getDouble("distance")),
                vectorLiteral, vectorLiteral, topK);
    }

    /** vd [0.12,-0.34,0.05] — định dạng text mà pgvector chấp nhận qua ép kiểu "::vector". */
    private static String toVectorLiteral(float[] embedding) {
        StringBuilder sb = new StringBuilder(embedding.length * 10);
        sb.append('[');
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(embedding[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
