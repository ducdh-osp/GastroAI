package vn.gastroai.be.application.rag;

import org.junit.jupiter.api.Test;
import vn.gastroai.be.application.triage.TriageService;
import vn.gastroai.be.domain.triage.TriageResult;
import vn.gastroai.be.infrastructure.ai.GeminiChatClient;
import vn.gastroai.be.infrastructure.ai.GeminiEmbeddingClient;
import vn.gastroai.be.infrastructure.ai.GeminiStreamingChatClient;
import vn.gastroai.be.infrastructure.ai.ImagePart;
import vn.gastroai.be.infrastructure.rag.EmbeddingStore;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StreamingRagQueryServiceTest {

    @Test
    void streamAnswerRedactsDosageFromAttachedDocumentInSystemPrompt() {
        GeminiEmbeddingClient embeddingClient = mock(GeminiEmbeddingClient.class);
        EmbeddingStore embeddingStore = mock(EmbeddingStore.class);
        GeminiStreamingChatClient streamingChatClient = mock(GeminiStreamingChatClient.class);
        GeminiChatClient chatClient = mock(GeminiChatClient.class);
        TriageService triageService = mock(TriageService.class);

        float[] queryVector = {0.1f, 0.2f};
        when(triageService.check(anyString())).thenReturn(TriageResult.safe());
        when(embeddingClient.embed(anyString())).thenReturn(queryVector);
        when(embeddingStore.findTopK(queryVector, 5)).thenReturn(List.of());

        doAnswer(invocation -> {
            Consumer<String> onToken = invocation.getArgument(3);
            onToken.accept("Ban nen di kham de duoc ke don phu hop.");
            return null;
        }).when(streamingChatClient).generateStream(anyString(), anyString(), any(), any());

        StreamingRagQueryService service = new StreamingRagQueryService(
                embeddingClient, embeddingStore, streamingChatClient, chatClient, triageService, 5);

        service.streamAnswer(
                "Thuoc nay uong lieu bao nhieu?",
                List.of(),
                "Amitriptyline 25mg/ngay vao buoi toi.",
                token -> { },
                triage -> { }
        );

        // System prompt dua cho Gemini streaming phai da duoc che lieu, khong chi
        // hien thi cho nguoi dung ma con phai loc ngay tu dau vao cua model.
        verify(streamingChatClient).generateStream(
                argThat(systemPrompt -> !systemPrompt.contains("25mg")),
                eq("Thuoc nay uong lieu bao nhieu?"),
                eq(List.of()),
                any());
    }

    @Test
    void streamAnswerPassesImagesThroughToStreamingClient() {
        GeminiEmbeddingClient embeddingClient = mock(GeminiEmbeddingClient.class);
        EmbeddingStore embeddingStore = mock(EmbeddingStore.class);
        GeminiStreamingChatClient streamingChatClient = mock(GeminiStreamingChatClient.class);
        GeminiChatClient chatClient = mock(GeminiChatClient.class);
        TriageService triageService = mock(TriageService.class);

        float[] queryVector = {0.1f, 0.2f};
        when(triageService.check(anyString())).thenReturn(TriageResult.safe());
        when(embeddingClient.embed(anyString())).thenReturn(queryVector);
        when(embeddingStore.findTopK(queryVector, 5)).thenReturn(List.of());

        doAnswer(invocation -> {
            Consumer<String> onToken = invocation.getArgument(3);
            onToken.accept("Day la mo ta anh.");
            return null;
        }).when(streamingChatClient).generateStream(anyString(), anyString(), any(), any());

        StreamingRagQueryService service = new StreamingRagQueryService(
                embeddingClient, embeddingStore, streamingChatClient, chatClient, triageService, 5);

        List<ImagePart> images = List.of(new ImagePart("image/png", "AQID"));

        StreamingRagQueryService.StreamingResult result = service.streamAnswer(
                "Day la vet gi tren da?",
                images,
                null,
                token -> { },
                triage -> { }
        );

        assertEquals("Day la mo ta anh.", result.answer());
        verify(streamingChatClient).generateStream(anyString(), eq("Day la vet gi tren da?"), eq(images), any());
    }
}