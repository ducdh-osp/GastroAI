package vn.gastroai.be.infrastructure.rag;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Xac nhan findTopK() chi tra ve chunk thuoc document status='DONE', khong lo chunk cua
 * document ERROR vao - du 2 document co embedding GIONG HET NHAU (cung 1 vector toan so 1),
 * nen neu ai xoa dieu kien WHERE d.status = 'DONE' thi chunk ERROR chac chan lot vao top-10.
 *
 * Chay: RUN_DB_IT=true ./mvnw test -Dtest=EmbeddingStoreStatusFilterIT
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RUN_DB_IT", matches = "true")
class EmbeddingStoreStatusFilterIT {

    private static final String TITLE_PREFIX = "EmbeddingStoreStatusFilterIT - ";

    @Autowired
    @Qualifier("postgresDataSource")
    private DataSource postgresDataSource;

    @Autowired
    private EmbeddingStore embeddingStore;

    private JdbcTemplate jdbcTemplate;
    private Long doneChunkId;
    private Long errorChunkId;

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(postgresDataSource);

        Long doneDocumentId = jdbcTemplate.queryForObject(
                "INSERT INTO documents (title, status) VALUES (?, 'DONE') RETURNING id",
                Long.class, TITLE_PREFIX + "DONE");
        doneChunkId = jdbcTemplate.queryForObject(
                "INSERT INTO chunks (document_id, chunk_index, content) VALUES (?, 0, ?) RETURNING id",
                Long.class, doneDocumentId, "Noi dung chunk DONE");

        Long errorDocumentId = jdbcTemplate.queryForObject(
                "INSERT INTO documents (title, status) VALUES (?, 'ERROR') RETURNING id",
                Long.class, TITLE_PREFIX + "ERROR");
        errorChunkId = jdbcTemplate.queryForObject(
                "INSERT INTO chunks (document_id, chunk_index, content) VALUES (?, 0, ?) RETURNING id",
                Long.class, errorDocumentId, "Noi dung chunk ERROR");

        float[] vector = new float[3072];
        Arrays.fill(vector, 1f);
        embeddingStore.save(doneChunkId, vector, "test-model");
        embeddingStore.save(errorChunkId, vector, "test-model");
    }

    @AfterEach
    void tearDown() {
        // ON DELETE CASCADE (xem migration V7) tu don luon chunks + embeddings lien quan.
        jdbcTemplate.update("DELETE FROM documents WHERE title LIKE ?", TITLE_PREFIX + "%");
    }

    @Test
    void findTopKOnlyReturnsChunksFromDoneDocuments() {
        float[] queryVector = new float[3072];
        Arrays.fill(queryVector, 1f);

        List<SimilarChunk> results = embeddingStore.findTopK(queryVector, 10);

        assertTrue(results.stream().anyMatch(r -> r.chunkId().equals(doneChunkId)),
                "Phai co chunk cua document DONE");
        assertFalse(results.stream().anyMatch(r -> r.chunkId().equals(errorChunkId)),
                "Khong duoc co chunk cua document ERROR");
    }
}