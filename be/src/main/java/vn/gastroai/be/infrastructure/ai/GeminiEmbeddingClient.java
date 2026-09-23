package vn.gastroai.be.infrastructure.ai;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import vn.gastroai.be.config.GeminiProperties;

import java.util.List;
import java.util.Map;

/**
 * UC0033 - gọi Gemini embedContent để chuyển 1 đoạn text thành vector. Số chiều trả về (3072
 * với model "gemini-embedding-2") PHẢI khớp đúng cột vector(3072) khai trong migration V7 —
 * đã xác nhận thực tế qua API, không phải số đoán (xem be/README.md).
 */
@Component
public class GeminiEmbeddingClient {
    private final RestClient restClient;
    private final String model;

    public GeminiEmbeddingClient(RestClient geminiRestClient, GeminiProperties properties) {
        this.restClient = geminiRestClient;
        this.model = properties.embeddingModel();
    }

    public float[] embed(String text) {
        Map<String, Object> body = Map.of(
                "content", Map.of("parts", List.of(Map.of("text", text))));

        EmbedResponse response = restClient.post()
                .uri("/models/{model}:embedContent", model)
                .body(body)
                .retrieve()
                .body(EmbedResponse.class);

        if (response == null || response.embedding() == null) {
            throw new IllegalStateException("Gemini khong tra ve embedding cho text da cho");
        }
        return response.embedding().values();
    }

    // Chỉ khai đúng field cần dùng — Jackson bỏ qua field JSON thừa mà không cần cấu hình gì.
    private record EmbedResponse(EmbeddingValues embedding) {
    }

    private record EmbeddingValues(float[] values) {
    }
}
