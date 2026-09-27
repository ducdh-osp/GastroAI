package vn.gastroai.be.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import vn.gastroai.be.config.GeminiProperties;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class GeminiStreamingChatClient {

    private final RestClient restClient;
    private final String model;
    private final ObjectMapper objectMapper;

    public GeminiStreamingChatClient(
            RestClient geminiRestClient,
            GeminiProperties properties
    ) {
        this.restClient = geminiRestClient;
        this.model = properties.chatModel();
        this.objectMapper = new ObjectMapper();
    }

    public void generateStream(
            String systemPrompt,
            String userPrompt,
            Consumer<String> onToken
    ) {
        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of(
                        "parts", List.of(
                                Map.of("text", systemPrompt)
                        )
                ),
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(
                                        Map.of("text", userPrompt)
                                )
                        )
                )
        );

        restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/models/{model}:streamGenerateContent")
                        .queryParam("alt", "sse")
                        .build(model))
                .body(body)
                .exchange((request, response) -> {

                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw new IllegalStateException(
                                "Gemini streaming failed: HTTP "
                                        + response.getStatusCode()
                        );
                    }

                    InputStream inputStream = response.getBody();

                    if (inputStream == null) {
                        throw new IllegalStateException(
                                "Gemini streaming không trả về response body"
                        );
                    }

                    readSseStream(inputStream, onToken);

                    return null;
                });
    }

    private void readSseStream(
            InputStream inputStream,
            Consumer<String> onToken
    ) {
        try (
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        inputStream,
                                        StandardCharsets.UTF_8
                                )
                        )
        ) {
            String line;

            while ((line = reader.readLine()) != null) {

                // Chỉ xử lý SSE data event
                if (!line.startsWith("data:")) {
                    continue;
                }

                String json = line.substring(5).trim();

                // Bỏ qua data event rỗng
                if (json.isEmpty()) {
                    continue;
                }

                JsonNode root;

                try {
                    root = objectMapper.readTree(json);
                } catch (Exception exception) {
                    // Một data event lỗi không được làm chết toàn bộ stream.
                    continue;
                }

                JsonNode candidates = root.path("candidates");

                if (!candidates.isArray()
                        || candidates.isEmpty()) {
                    continue;
                }

                JsonNode parts = candidates
                        .get(0)
                        .path("content")
                        .path("parts");

                if (!parts.isArray()
                        || parts.isEmpty()) {
                    continue;
                }

                JsonNode textNode = parts
                        .get(0)
                        .path("text");

                if (!textNode.isTextual()) {
                    continue;
                }

                String text = textNode.asText();

                if (!text.isEmpty()) {
                    onToken.accept(text);
                }
            }

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Lỗi khi đọc Gemini streaming response",
                    exception
            );
        }
    }
}