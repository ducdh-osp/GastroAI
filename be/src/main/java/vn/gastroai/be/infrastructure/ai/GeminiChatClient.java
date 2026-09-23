package vn.gastroai.be.infrastructure.ai;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import vn.gastroai.be.config.GeminiProperties;

import java.util.List;
import java.util.Map;

/** UC0029 - gọi Gemini generateContent để sinh câu trả lời dựa trên ngữ cảnh đã truy xuất. */
@Component
public class GeminiChatClient {
    private final RestClient restClient;
    private final String model;

    public GeminiChatClient(RestClient geminiRestClient, GeminiProperties properties) {
        this.restClient = geminiRestClient;
        this.model = properties.chatModel();
    }

    /**
     * systemPrompt mang theo ngữ cảnh RAG đã truy xuất (UC0028) + hướng dẫn cách trả lời;
     * userPrompt là câu hỏi thật của người dùng. Tách riêng 2 phần để dễ audit/tinh chỉnh
     * prompt sau này mà không phải sửa logic gọi API.
     */
    public String generate(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", userPrompt)))));

        GenerateResponse response = restClient.post()
                .uri("/models/{model}:generateContent", model)
                .body(body)
                .retrieve()
                .body(GenerateResponse.class);

        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new IllegalStateException("Gemini khong tra ve cau tra loi nao");
        }
        return response.candidates().get(0).content().parts().get(0).text();
    }

    private record GenerateResponse(List<Candidate> candidates) {
    }

    private record Candidate(Content content) {
    }

    private record Content(List<Part> parts) {
    }

    private record Part(String text) {
    }
}
