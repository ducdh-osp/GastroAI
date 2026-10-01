package vn.gastroai.be.application.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.gastroai.be.infrastructure.ai.GeminiChatClient;
import vn.gastroai.be.infrastructure.ai.GeminiEmbeddingClient;
import vn.gastroai.be.infrastructure.ai.GeminiStreamingChatClient;
import vn.gastroai.be.infrastructure.rag.EmbeddingStore;
import vn.gastroai.be.infrastructure.rag.SimilarChunk;
import vn.gastroai.be.application.triage.TriageService;
import vn.gastroai.be.domain.triage.TriageResult;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class StreamingRagQueryService {

    private static final int MAX_DISPLAYED_SOURCES = 2;

    private final GeminiEmbeddingClient embeddingClient;
    private final EmbeddingStore embeddingStore;
    private final GeminiStreamingChatClient streamingChatClient;
    private final GeminiChatClient chatClient;
    private final TriageService triageService;
    private final int topK;

    public StreamingRagQueryService(
            GeminiEmbeddingClient embeddingClient,
            EmbeddingStore embeddingStore,
            GeminiStreamingChatClient streamingChatClient,
            GeminiChatClient chatClient,
            TriageService triageService,
            @Value("${app.rag.top-k:5}") int topK
    ) {
        this.embeddingClient = embeddingClient;
        this.embeddingStore = embeddingStore;
        this.streamingChatClient = streamingChatClient;
        this.chatClient = chatClient;
        this.triageService = triageService;
        this.topK = topK;
    }

    public StreamingResult streamAnswer(
            String question,
            Consumer<String> onToken,
            Consumer<TriageResult> onTriageChecked
    ) {
        TriageResult triageResult = triageService.check(question);

        onTriageChecked.accept(triageResult);

        float[] queryVector = embeddingClient.embed(question);

        List<SimilarChunk> context = embeddingStore.findTopK(queryVector, topK);

        List<RagSource> sources = context.stream()
                .limit(MAX_DISPLAYED_SOURCES)
                .map(chunk -> new RagSource(chunk.documentTitle(), chunk.content(), chunk.sourceUrl()))
                .toList();

        String systemPrompt;

        if (context.isEmpty()) {
            systemPrompt = SYSTEM_PROMPT_NO_CONTEXT;
        } else {
            String contextText = context.stream()
                    .map(chunk -> "- " + chunk.content())
                    .collect(Collectors.joining("\n"));
            systemPrompt = SYSTEM_PROMPT_PREFIX + contextText;
        }

        StringBuilder answer = new StringBuilder();

        streamingChatClient.generateStream(
                systemPrompt,
                question,
                token -> {
                    answer.append(token);
                    onToken.accept(token);
                }
        );

        List<String> relatedQuestions = generateRelatedQuestions(question, answer.toString());

        return new StreamingResult(
                answer.toString(),
                sources,
                relatedQuestions,
                triageResult.emergency(),
                triageResult.matchedGroups()
        );
    }

    private List<String> generateRelatedQuestions(String question, String answer) {
        try {
            String userPrompt = "Cau hoi: " + question + "\nCau tra loi: " + answer;

            String raw = chatClient.generate(SYSTEM_PROMPT_RELATED_QUESTIONS, userPrompt);

            return raw.lines()
                    .map(line -> line.replaceFirst("^[-*\\d.)\\s]+", "").trim())
                    .filter(line -> !line.isBlank())
                    .limit(3)
                    .toList();

        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private static final String SYSTEM_PROMPT_PREFIX = """
            Ban la tro ly AI cua GastroAI, chuyen tu van ve suc khoe tieu hoa. CHI tra loi dua \
            tren ngu canh duoc cung cap ben duoi, khong tu bia them thong tin y khoa. Neu ngu \
            canh khong du de tra loi, hay noi ro dieu do va khuyen nguoi dung gap bac si. Luon \
            nhac nguoi dung day chi la thong tin tham khao, khong thay the chan doan y te.

            Ngu canh:
            """;

    private static final String SYSTEM_PROMPT_NO_CONTEXT = """
            Ban la tro ly AI cua GastroAI, chuyen tu van ve suc khoe tieu hoa. Kho tri thuc \
            hien chua co tai lieu nao lien quan. Hay noi ro la chua co du lieu de tra loi \
            chinh xac, va khuyen nguoi dung gap bac si neu can thiet. Khong tu bia thong tin y khoa.
            """;

    private static final String SYSTEM_PROMPT_RELATED_QUESTIONS = """
            Dua tren cau hoi va cau tra loi tu van suc khoe tieu hoa duoi day, hay de xuat dung \
            3 cau hoi lien quan ma nguoi dung co the muon hoi tiep. Moi cau hoi ngan gon, viet \
            tren 1 dong rieng, KHONG danh so, KHONG them giai thich hay ky tu nao khac ngoai \
            chinh cau hoi.
            """;

    public record StreamingResult(
            String answer,
            List<RagSource> sources,
            List<String> relatedQuestions,
            boolean emergency,
            List<String> matchedGroups
    ) {
    }
}