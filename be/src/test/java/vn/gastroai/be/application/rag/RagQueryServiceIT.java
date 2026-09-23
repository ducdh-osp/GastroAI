package vn.gastroai.be.application.rag;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.gastroai.be.infrastructure.ai.GeminiEmbeddingClient;
import vn.gastroai.be.infrastructure.rag.EmbeddingStore;
import vn.gastroai.be.infrastructure.rag.SimilarChunk;

import javax.sql.DataSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test tự tay chạy để xác nhận UC0028 (truy xuất) + UC0029 (sinh câu trả lời) + UC0033
 * (embedding) hoạt động đúng xuyên suốt, gọi API Gemini THẬT (không mock) nên cần
 * GEMINI_API_KEY thật — tự bỏ qua nếu chưa set biến này (vd máy CI/đồng đội chưa có key),
 * không làm fail cả build.
 *
 * Chạy: GEMINI_API_KEY=... mvn test -Dtest=RagQueryServiceIT
 *
 * Tự tạo + xóa 1 document/chunk tạm qua SQL thuần (không dùng entity Document/Chunk — 2 bảng
 * đó thuộc UC0031/032 của Thăng, chưa có entity Java, chỉ cần tồn tại đúng schema để test).
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
class RagQueryServiceIT {

    private static final String TEST_DOCUMENT_TITLE = "RagQueryServiceIT - tai lieu test tam";

    @Autowired
    private GeminiEmbeddingClient embeddingClient;

    @Autowired
    private EmbeddingStore embeddingStore;

    @Autowired
    private RagQueryService ragQueryService;

    @Autowired
    @Qualifier("postgresDataSource")
    private DataSource postgresDataSource;

    private JdbcTemplate jdbcTemplate;
    private Long testChunkId;

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(postgresDataSource);

        Long documentId = jdbcTemplate.queryForObject(
                "INSERT INTO documents (title, status) VALUES (?, 'DONE') RETURNING id",
                Long.class, TEST_DOCUMENT_TITLE);
        testChunkId = jdbcTemplate.queryForObject(
                "INSERT INTO chunks (document_id, chunk_index, content) VALUES (?, 0, ?) RETURNING id",
                Long.class, documentId,
                "Uong nhieu nuoc va an nhieu chat xo giup giam trieu chung tao bon.");
    }

    @AfterEach
    void tearDown() {
        // ON DELETE CASCADE (xem migration V7) tự dọn luôn chunks + embeddings liên quan.
        jdbcTemplate.update("DELETE FROM documents WHERE title = ?", TEST_DOCUMENT_TITLE);
    }

    @Test
    void embedStoreRetrieveAndAnswer() {
        // UC0033 - sinh embedding thật từ Gemini, đúng 3072 chiều như đã xác nhận qua API.
        float[] embedding = embeddingClient.embed(
                "Uong nhieu nuoc va an nhieu chat xo giup giam trieu chung tao bon.");
        assertEquals(3072, embedding.length);

        embeddingStore.save(testChunkId, embedding, "gemini-embedding-2");

        // UC0028 - truy xuất lại đúng chunk vừa lưu bằng chính vector của nó (phải là kết quả
        // gần nhất tuyệt đối, distance ~ 0).
        List<SimilarChunk> results = embeddingStore.findTopK(embedding, 1);
        assertFalse(results.isEmpty(), "Phai tim thay it nhat 1 ket qua");
        assertEquals(testChunkId, results.get(0).chunkId());

        // UC0029 - sinh câu trả lời thật dựa trên ngữ cảnh vừa lưu.
        String answer = ragQueryService.answer("Lam sao de giam tao bon?");
        assertTrue(answer != null && !answer.isBlank(), "Gemini phai tra ve cau tra loi");
        System.out.println("=== Cau tra loi cua Gemini ===\n" + answer);
    }
}
