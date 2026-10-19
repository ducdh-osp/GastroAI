package vn.gastroai.be.application.rag;

import org.junit.jupiter.api.Test;
import vn.gastroai.be.application.config.AppRuntimeConfigService;
import vn.gastroai.be.application.config.AppRuntimeConfigSnapshot;
import vn.gastroai.be.infrastructure.ai.GeminiChatClient;
import vn.gastroai.be.infrastructure.ai.GeminiEmbeddingClient;
import vn.gastroai.be.infrastructure.rag.EmbeddingStore;
import vn.gastroai.be.infrastructure.rag.SimilarChunk;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
                new SimilarChunk(1L, 10L, "Cam nang tieu hoa", "Uong nhieu nuoc va an nhieu chat xo.", 0.05, null)));
        when(chatClient.generate(anyString(), anyString(), any())).thenReturn("Ban nen uong nhieu nuoc.");

        RagQueryService ragQueryService = ragQueryServiceWithTopK(embeddingClient, embeddingStore, chatClient, 5);

        RagAnswer result = ragQueryService.answerWithSources("Lam sao de giam tao bon?");

        assertEquals("Ban nen uong nhieu nuoc.", result.answer());
        assertEquals(1, result.sources().size());
        assertEquals("Cam nang tieu hoa", result.sources().get(0).documentTitle());
        assertEquals("Uong nhieu nuoc va an nhieu chat xo.", result.sources().get(0).snippet());
    }

    @Test
    void answerWithSourcesRedactsDosageFromSourceSnippetAndPromptContext() {
        GeminiEmbeddingClient embeddingClient = mock(GeminiEmbeddingClient.class);
        EmbeddingStore embeddingStore = mock(EmbeddingStore.class);
        GeminiChatClient chatClient = mock(GeminiChatClient.class);

        float[] queryVector = {0.1f, 0.2f};
        when(embeddingClient.embed(anyString())).thenReturn(queryVector);
        when(embeddingStore.findTopK(queryVector, 5)).thenReturn(List.of(
                new SimilarChunk(1L, 10L, "Phac do dieu tri", "Amitriptyline 25mg/ngay vao buoi toi.", 0.05, null)));
        when(chatClient.generate(anyString(), anyString(), any())).thenReturn("Ban nen di kham de duoc ke don phu hop.");

        RagQueryService ragQueryService = ragQueryServiceWithTopK(embeddingClient, embeddingStore, chatClient, 5);

        RagAnswer result = ragQueryService.answerWithSources("Thuoc nay uong lieu bao nhieu?");

        assertFalse(result.sources().get(0).snippet().contains("25mg"));
        // Prompt gui Gemini cung phai duoc loc, khong chi snippet hien thi cho benh nhan.
        verify(chatClient).generate(argThat(prompt -> !prompt.contains("25mg")), eq("Thuoc nay uong lieu bao nhieu?"), any());
    }

    @Test
    void answerWithSourcesReturnsEmptySourcesWhenNoContextFound() {
        GeminiEmbeddingClient embeddingClient = mock(GeminiEmbeddingClient.class);
        EmbeddingStore embeddingStore = mock(EmbeddingStore.class);
        GeminiChatClient chatClient = mock(GeminiChatClient.class);

        float[] queryVector = {0.1f, 0.2f};
        when(embeddingClient.embed(anyString())).thenReturn(queryVector);
        when(embeddingStore.findTopK(queryVector, 5)).thenReturn(List.of());
        when(chatClient.generate(anyString(), anyString(), any())).thenReturn("Chua co du lieu de tra loi.");

        RagQueryService ragQueryService = ragQueryServiceWithTopK(embeddingClient, embeddingStore, chatClient, 5);

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
                new SimilarChunk(1L, 10L, "Cam nang tieu hoa", "Uong nhieu nuoc.", 0.05, null)));
        when(chatClient.generate(anyString(), eq("Dau bung phai lam sao?"), any()))
                .thenReturn("Ban nen uong nhieu nuoc.");
        // Gemini dôi khi tra ve co danh so/gach dau dong du da yeu cau khong lam vay - phai
        // tu lam sach thay vi tin tuong tuyet doi vao prompt.
        when(chatClient.generate(anyString(), eq("Cau hoi: Dau bung phai lam sao?\nCau tra loi: Ban nen uong nhieu nuoc.")))
                .thenReturn("1. Trieu chung nay co nguy hiem khong?\n- Khi nao nen di kham?\n\nToi nen theo doi gi them?");

        RagQueryService ragQueryService = ragQueryServiceWithTopK(embeddingClient, embeddingStore, chatClient, 5);

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
        when(chatClient.generate(anyString(), eq("Cau hoi khong co trong kho tri thuc"), any()))
                .thenReturn("Chua co du lieu de tra loi.");
        when(chatClient.generate(anyString(), eq("Cau hoi: Cau hoi khong co trong kho tri thuc\nCau tra loi: Chua co du lieu de tra loi.")))
                .thenThrow(new IllegalStateException("Gemini khong tra ve cau tra loi nao"));

        RagQueryService ragQueryService = ragQueryServiceWithTopK(embeddingClient, embeddingStore, chatClient, 5);

        RagAnswer result = ragQueryService.answerWithSources("Cau hoi khong co trong kho tri thuc");

        // Cau tra loi chinh van phai nguyen ven du buoc goi y cau hoi lien quan bi loi.
        assertEquals("Chua co du lieu de tra loi.", result.answer());
        assertTrue(result.relatedQuestions().isEmpty());
    }

    @Test
    void answerWithSourcesUsesRewrittenSnippetWhenGeminiReturnsExpectedFormat() {
        GeminiEmbeddingClient embeddingClient = mock(GeminiEmbeddingClient.class);
        EmbeddingStore embeddingStore = mock(EmbeddingStore.class);
        GeminiChatClient chatClient = mock(GeminiChatClient.class);

        float[] queryVector = {0.1f, 0.2f};
        when(embeddingClient.embed(anyString())).thenReturn(queryVector);
        when(embeddingStore.findTopK(queryVector, 5)).thenReturn(List.of(
                new SimilarChunk(1L, 10L, "Cam nang tieu hoa", "trieu chung kho chiu vi tri thuong gap", 0.05, null)));
        when(chatClient.generate(anyString(), anyString(), any())).thenReturn("Ban nen di kham.");
        when(chatClient.generate(anyString(), eq("===1===\ntrieu chung kho chiu vi tri thuong gap\n")))
                .thenReturn("===1===\nTrieu chung kho chiu o vi tri thuong gap.\n");

        RagQueryService ragQueryService = ragQueryServiceWithTopK(embeddingClient, embeddingStore, chatClient, 5);

        RagAnswer result = ragQueryService.answerWithSources("Dau bung la sao?");

        assertEquals("Trieu chung kho chiu o vi tri thuong gap.", result.sources().get(0).snippet());
    }

    @Test
    void answerWithSourcesFallsBackToOriginalSnippetWhenRewriteFormatIsMalformed() {
        GeminiEmbeddingClient embeddingClient = mock(GeminiEmbeddingClient.class);
        EmbeddingStore embeddingStore = mock(EmbeddingStore.class);
        GeminiChatClient chatClient = mock(GeminiChatClient.class);

        float[] queryVector = {0.1f, 0.2f};
        when(embeddingClient.embed(anyString())).thenReturn(queryVector);
        when(embeddingStore.findTopK(queryVector, 5)).thenReturn(List.of(
                new SimilarChunk(1L, 10L, "Cam nang tieu hoa", "noi dung goc khong bi doi", 0.05, null)));
        when(chatClient.generate(anyString(), anyString(), any())).thenReturn("Ban nen di kham.");
        // Thieu dung dinh dang "===N===" - phai giu nguyen ban goc, khong doan mo noi dung.
        when(chatClient.generate(anyString(), eq("===1===\nnoi dung goc khong bi doi\n")))
                .thenReturn("Xin loi, toi khong hieu yeu cau.");

        RagQueryService ragQueryService = ragQueryServiceWithTopK(embeddingClient, embeddingStore, chatClient, 5);

        RagAnswer result = ragQueryService.answerWithSources("Dau bung la sao?");

        assertEquals("noi dung goc khong bi doi", result.sources().get(0).snippet());
    }

    @Test
    void answerWithSourcesRedactsDosageFromRewrittenSnippetToo() {
        GeminiEmbeddingClient embeddingClient = mock(GeminiEmbeddingClient.class);
        EmbeddingStore embeddingStore = mock(EmbeddingStore.class);
        GeminiChatClient chatClient = mock(GeminiChatClient.class);

        float[] queryVector = {0.1f, 0.2f};
        when(embeddingClient.embed(anyString())).thenReturn(queryVector);
        when(embeddingStore.findTopK(queryVector, 5)).thenReturn(List.of(
                new SimilarChunk(1L, 10L, "Phac do dieu tri", "uong thuoc theo chi dinh", 0.05, null)));
        when(chatClient.generate(anyString(), anyString(), any())).thenReturn("Ban nen di kham.");
        // Gia lap truong hop Gemini "lam muot" cau chu nhung lo dien lai lieu luong - phong ngua
        // bang cach che lieu LAN NUA tren chinh van ban da viet lai, khong chi tin ban goc da
        // sach la xong.
        when(chatClient.generate(anyString(), eq("===1===\nuong thuoc theo chi dinh\n")))
                .thenReturn("===1===\nUong 25mg moi ngay theo chi dinh.\n");

        RagQueryService ragQueryService = ragQueryServiceWithTopK(embeddingClient, embeddingStore, chatClient, 5);

        RagAnswer result = ragQueryService.answerWithSources("Thuoc nay uong the nao?");

        assertFalse(result.sources().get(0).snippet().contains("25mg"));
    }

    /** UC0059 - topK gio doc tu AppRuntimeConfigService (co the doi qua CMS) thay vi hardcode -
     * mock san 1 config co dinh (threshold=null = khong loc gi them) cho cac test logic RAG o
     * tren, vi chung khong nham kiem tra UC0059. */
    private static RagQueryService ragQueryServiceWithTopK(
            GeminiEmbeddingClient embeddingClient, EmbeddingStore embeddingStore,
            GeminiChatClient chatClient, int topK) {
        AppRuntimeConfigService configService = mock(AppRuntimeConfigService.class);
        when(configService.getConfig()).thenReturn(new AppRuntimeConfigSnapshot(topK, null, true, null, null));
        return new RagQueryService(embeddingClient, embeddingStore, chatClient, configService);
    }
}
