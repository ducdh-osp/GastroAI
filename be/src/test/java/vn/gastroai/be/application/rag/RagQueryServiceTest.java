package vn.gastroai.be.application.rag;

import org.junit.jupiter.api.Test;
import vn.gastroai.be.infrastructure.ai.GeminiChatClient;
import vn.gastroai.be.infrastructure.ai.GeminiEmbeddingClient;
import vn.gastroai.be.infrastructure.rag.EmbeddingStore;
import vn.gastroai.be.infrastructure.rag.SimilarChunk;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RagQueryServiceTest {

    @Test
    void answerWithSourcesMapsRetrievedChunksToSources() {
        GeminiEmbeddingClient embeddingClient = mock(GeminiEmbeddingClient.class);
        EmbeddingStore embeddingStore = mock(EmbeddingStore.class);
        GeminiChatClient chatClient = mock(GeminiChatClient.class);

        float[] queryVector = {0.1f, 0.2f};
        when(embeddingClient.embed("Lam sao de giam tao bon?")).thenReturn(queryVector);
        when(embeddingStore.findTopK(queryVector, 5)).thenReturn(List.of(
                new SimilarChunk(1L, 10L, "Cam nang tieu hoa", "Uong nhieu nuoc va an nhieu chat xo.", 0.05)));
        when(chatClient.generate(anyString(), anyString())).thenReturn("Ban nen uong nhieu nuoc.");

        RagQueryService ragQueryService = new RagQueryService(embeddingClient, embeddingStore, chatClient, 5);

        RagAnswer result = ragQueryService.answerWithSources("Lam sao de giam tao bon?");

        assertEquals("Ban nen uong nhieu nuoc.", result.answer());
        assertEquals(1, result.sources().size());
        assertEquals("Cam nang tieu hoa", result.sources().get(0).documentTitle());
        assertEquals("Uong nhieu nuoc va an nhieu chat xo.", result.sources().get(0).snippet());
    }

    @Test
    void answerWithSourcesReturnsEmptySourcesWhenNoContextFound() {
        GeminiEmbeddingClient embeddingClient = mock(GeminiEmbeddingClient.class);
        EmbeddingStore embeddingStore = mock(EmbeddingStore.class);
        GeminiChatClient chatClient = mock(GeminiChatClient.class);

        float[] queryVector = {0.1f, 0.2f};
        when(embeddingClient.embed(anyString())).thenReturn(queryVector);
        when(embeddingStore.findTopK(queryVector, 5)).thenReturn(List.of());
        when(chatClient.generate(anyString(), anyString())).thenReturn("Chua co du lieu de tra loi.");

        RagQueryService ragQueryService = new RagQueryService(embeddingClient, embeddingStore, chatClient, 5);

        RagAnswer result = ragQueryService.answerWithSources("Cau hoi khong co trong kho tri thuc");

        assertEquals("Chua co du lieu de tra loi.", result.answer());
        assertTrue(result.sources().isEmpty());
    }

    @Test
    void answerWithSourcesParsesRelatedQuestionsFromGeminiResponse() {
        GeminiEmbeddingClient embeddingClient = mock(GeminiEmbeddingClient.class);
        EmbeddingStore embeddingStore = mock(EmbeddingStore.class);
        GeminiChatClient chatClient = mock(GeminiChatClient.class);

        float[] queryVector = {0.1f, 0.2f};
        when(embeddingClient.embed(anyString())).thenReturn(queryVector);
        when(embeddingStore.findTopK(queryVector, 5)).thenReturn(List.of(
                new SimilarChunk(1L, 10L, "Cam nang tieu hoa", "Uong nhieu nuoc.", 0.05)));
        when(chatClient.generate(anyString(), eq("Dau bung phai lam sao?")))
                .thenReturn("Ban nen uong nhieu nuoc.");
        // Gemini dôi khi tra ve co danh so/gach dau dong du da yeu cau khong lam vay - phai
        // tu lam sach thay vi tin tuong tuyet doi vao prompt.
        when(chatClient.generate(anyString(), eq("Cau hoi: Dau bung phai lam sao?\nCau tra loi: Ban nen uong nhieu nuoc.")))
                .thenReturn("1. Trieu chung nay co nguy hiem khong?\n- Khi nao nen di kham?\n\nToi nen theo doi gi them?");

        RagQueryService ragQueryService = new RagQueryService(embeddingClient, embeddingStore, chatClient, 5);

        RagAnswer result = ragQueryService.answerWithSources("Dau bung phai lam sao?");

        assertEquals(List.of(
                "Trieu chung nay co nguy hiem khong?",
                "Khi nao nen di kham?",
                "Toi nen theo doi gi them?"), result.relatedQuestions());
    }

    @Test
    void answerWithSourcesReturnsEmptyRelatedQuestionsWhenGeminiFailsOnThatStep() {
        GeminiEmbeddingClient embeddingClient = mock(GeminiEmbeddingClient.class);
        EmbeddingStore embeddingStore = mock(EmbeddingStore.class);
        GeminiChatClient chatClient = mock(GeminiChatClient.class);

        float[] queryVector = {0.1f, 0.2f};
        when(embeddingClient.embed(anyString())).thenReturn(queryVector);
        when(embeddingStore.findTopK(queryVector, 5)).thenReturn(List.of());
        when(chatClient.generate(anyString(), eq("Cau hoi khong co trong kho tri thuc")))
                .thenReturn("Chua co du lieu de tra loi.");
        when(chatClient.generate(anyString(), eq("Cau hoi: Cau hoi khong co trong kho tri thuc\nCau tra loi: Chua co du lieu de tra loi.")))
                .thenThrow(new IllegalStateException("Gemini khong tra ve cau tra loi nao"));

        RagQueryService ragQueryService = new RagQueryService(embeddingClient, embeddingStore, chatClient, 5);

        RagAnswer result = ragQueryService.answerWithSources("Cau hoi khong co trong kho tri thuc");

        // Cau tra loi chinh van phai nguyen ven du buoc goi y cau hoi lien quan bi loi.
        assertEquals("Chua co du lieu de tra loi.", result.answer());
        assertTrue(result.relatedQuestions().isEmpty());
    }
}
