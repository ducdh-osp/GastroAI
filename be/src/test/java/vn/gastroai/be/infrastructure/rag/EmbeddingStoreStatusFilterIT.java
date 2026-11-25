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
 * Xac nhan findTopK() chi tra ve chunk cua tai lieu chatbot dang dung (da duoc Bien tap xac nhan
 * va khong bi go), khong lo chunk cua document ERROR / chua xac nhan / da go - du cac
 * document co embedding GIONG HET NHAU (cung 1 vector toan so 1), nen neu ai xoa dieu kien
 * WHERE thi chunk khong hop le chac chan lot vao top-10.
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
    private Long notApprovedChunkId;
    private Long unpublishedChunkId;

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(postgresDataSource);

        Long doneDocumentId = jdbcTemplate.queryForObject(
                "INSERT INTO documents (title, processing_stage, content_approved_at) VALUES (?, 'DONE', now()) RETURNING id",
                Long.class, TITLE_PREFIX + "DONE");
        doneChunkId = jdbcTemplate.queryForObject(
                "INSERT INTO chunks (document_id, chunk_index, content) VALUES (?, 0, ?) RETURNING id",
                Long.class, doneDocumentId, "Noi dung chunk DONE");

        Long errorDocumentId = jdbcTemplate.queryForObject(
                "INSERT INTO documents (title, processing_stage) VALUES (?, 'ERROR') RETURNING id",
                Long.class, TITLE_PREFIX + "ERROR");
        errorChunkId = jdbcTemplate.queryForObject(
                "INSERT INTO chunks (document_id, chunk_index, content) VALUES (?, 0, ?) RETURNING id",
                Long.class, errorDocumentId, "Noi dung chunk ERROR");

        // SCR-AD-04 - ban DONE nhung chatbot khong dung vi chua duoc Bien tap xac nhan.
        Long notApprovedDocumentId = jdbcTemplate.queryForObject(
                "INSERT INTO documents (title, processing_stage) VALUES (?, 'DONE') RETURNING id",
                Long.class, TITLE_PREFIX + "NOT_APPROVED");
        notApprovedChunkId = jdbcTemplate.queryForObject(
                "INSERT INTO chunks (document_id, chunk_index, content) VALUES (?, 0, ?) RETURNING id",
                Long.class, notApprovedDocumentId, "Noi dung chunk chua xac nhan");

        // UC0050 - tai lieu da xac nhan nhung Admin da go khoi chatbot.
        Long unpublishedDocumentId = jdbcTemplate.queryForObject(
                "INSERT INTO documents (title, processing_stage, content_approved_at, unpublished_at) "
                        + "VALUES (?, 'DONE', now(), now()) RETURNING id",
                Long.class, TITLE_PREFIX + "UNPUBLISHED");
        unpublishedChunkId = jdbcTemplate.queryForObject(
                "INSERT INTO chunks (document_id, chunk_index, content) VALUES (?, 0, ?) RETURNING id",
                Long.class, unpublishedDocumentId, "Noi dung chunk da go");

        float[] vector = new float[3072];
        Arrays.fill(vector, 1f);
        embeddingStore.save(doneChunkId, vector, "test-model");
        embeddingStore.save(errorChunkId, vector, "test-model");
        embeddingStore.save(notApprovedChunkId, vector, "test-model");
        embeddingStore.save(unpublishedChunkId, vector, "test-model");
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

    @Test
    void findTopKSkipsDocumentsNotInUse() {
        float[] queryVector = new float[3072];
        Arrays.fill(queryVector, 1f);

        List<SimilarChunk> results = embeddingStore.findTopK(queryVector, 10);

        assertTrue(results.stream().noneMatch(r -> r.chunkId().equals(notApprovedChunkId)),
                "Chunk cua tai lieu chua xac nhan khong duoc lot vao ket qua RAG");
        assertTrue(results.stream().noneMatch(r -> r.chunkId().equals(unpublishedChunkId)),
                "Chunk cua tai lieu da go khong duoc lot vao ket qua RAG");
    }
}