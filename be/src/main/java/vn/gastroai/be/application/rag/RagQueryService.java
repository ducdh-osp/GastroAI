package vn.gastroai.be.application.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.gastroai.be.infrastructure.ai.GeminiChatClient;
import vn.gastroai.be.infrastructure.ai.GeminiEmbeddingClient;
import vn.gastroai.be.infrastructure.rag.EmbeddingStore;
import vn.gastroai.be.infrastructure.rag.SimilarChunk;

import java.util.List;
import java.util.stream.Collectors;

/**
 * UC0028 + UC0029 nối liền nhau: sinh embedding cho câu hỏi -> truy xuất top-k chunk gần nhất
 * -> đưa vào prompt cho Gemini sinh câu trả lời. Đây là "lõi" RAG — chưa gắn API/controller
 * nào (đó là việc của UC0017 Chat với AI, ngoài phạm vi 3 UC này), chỉ cần gọi answer() là
 * test được toàn bộ pipeline.
 */
@Service
public class RagQueryService {
    private final GeminiEmbeddingClient embeddingClient;
    private final EmbeddingStore embeddingStore;
    private final GeminiChatClient chatClient;
    private final int topK;

    public RagQueryService(
            GeminiEmbeddingClient embeddingClient,
            EmbeddingStore embeddingStore,
            GeminiChatClient chatClient,
            @Value("${app.rag.top-k:5}") int topK) {
        this.embeddingClient = embeddingClient;
        this.embeddingStore = embeddingStore;
        this.chatClient = chatClient;
        this.topK = topK;
    }

    public String answer(String question) {
        float[] queryVector = embeddingClient.embed(question);
        List<SimilarChunk> context = embeddingStore.findTopK(queryVector, topK);

        if (context.isEmpty()) {
            // Chưa có tài liệu nào trong kho tri thức (hoặc UC0031/032 của Thăng chưa xong) —
            // vẫn trả lời được nhưng phải nói rõ KHÔNG có nguồn, tránh Gemini tự bịa thông tin
            // y tế mà không có căn cứ.
            return chatClient.generate(SYSTEM_PROMPT_NO_CONTEXT, question);
        }

        String contextText = context.stream()
                .map(chunk -> "- " + chunk.content())
                .collect(Collectors.joining("\n"));
        String systemPrompt = SYSTEM_PROMPT_PREFIX + contextText;
        return chatClient.generate(systemPrompt, question);
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
}
