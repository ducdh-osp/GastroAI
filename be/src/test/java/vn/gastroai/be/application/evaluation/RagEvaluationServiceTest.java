package vn.gastroai.be.application.evaluation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.gastroai.be.application.evaluation.RagEvaluationService.RagEvaluationReport;
import vn.gastroai.be.infrastructure.ai.GeminiEmbeddingClient;
import vn.gastroai.be.infrastructure.rag.EmbeddingStore;
import vn.gastroai.be.infrastructure.rag.SimilarChunk;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("UC0037: RAG Retrieval Accuracy Evaluation Tests")
class RagEvaluationServiceTest {

    private GeminiEmbeddingClient embeddingClient;
    private EmbeddingStore embeddingStore;
    private DataSource dataSource;
    private RagEvaluationService evaluationService;

    @BeforeEach
    void setUp() throws Exception {
        embeddingClient = mock(GeminiEmbeddingClient.class);
        embeddingStore = mock(EmbeddingStore.class);
        dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);

        evaluationService = new RagEvaluationService(embeddingClient, embeddingStore, dataSource);
    }

    @Test
    @DisplayName("Evaluate with mock vector retrieval successfully computes HitRate, MRR, Precision and Recall")
    void testVectorEvaluationMetrics() {
        float[] vector = new float[]{0.1f, 0.2f};
        when(embeddingClient.embed(anyString())).thenReturn(vector);

        // Giả lập kết quả tìm kiếm top-5 cho mỗi query: chunk đầu tiên khớp tài liệu kỳ vọng
        when(embeddingStore.findTopK(any(), anyInt())).thenAnswer(invocation -> {
            return List.of(
                    new SimilarChunk(101L, 1L, "01-trao-nguoc-da-day-thuc-quan.pdf",
                            "Tài liệu về trào ngược dạ dày thực quản GERD", 0.15, null),
                    new SimilarChunk(102L, 2L, "02-viem-loet-da-day-ta-trang-hp.pdf",
                            "Tài liệu về viêm loét dạ dày tá tràng và HP", 0.35, null)
            );
        });

        RagEvaluationReport report = evaluationService.evaluate(5);

        assertNotNull(report);
        assertEquals(RagEvaluationService.BENCHMARK_QUERIES.size(), report.totalQueries());
        assertEquals(5, report.topK());
        assertEquals("VECTOR", report.retrievalMode());
        assertTrue(report.hitCountAt1() >= 1, "Should have at least 1 hit at top 1");
        assertTrue(report.hitRateAt1() > 0.0);
        assertTrue(report.mrr() > 0.0);
        assertTrue(report.averageSimilarity() > 0.5);
        assertNotNull(report.queryResults());
        assertEquals(report.totalQueries(), report.queryResults().size());
    }

    @Test
    @DisplayName("Evaluate with lexical fallback when embedding service throws exception")
    void testLexicalFallbackEvaluation() {
        when(embeddingClient.embed(anyString())).thenThrow(new RuntimeException("Gemini API key not configured or quota exceeded"));

        RagEvaluationReport report = evaluationService.evaluate(5);

        assertNotNull(report);
        assertEquals(RagEvaluationService.BENCHMARK_QUERIES.size(), report.totalQueries());
        assertEquals("LEXICAL_FALLBACK", report.retrievalMode());
        assertTrue(report.hitRateAt1() >= 0.80, "Lexical fallback should achieve good hit rate on benchmark documents");
        assertTrue(report.mrr() >= 0.80);
    }

    @Test
    @DisplayName("Cached report is returned on getLatestReport")
    void testGetLatestReport() {
        RagEvaluationReport report1 = evaluationService.getLatestReport();
        assertNotNull(report1);
        RagEvaluationReport report2 = evaluationService.getLatestReport();
        assertSame(report1, report2);
    }
}
