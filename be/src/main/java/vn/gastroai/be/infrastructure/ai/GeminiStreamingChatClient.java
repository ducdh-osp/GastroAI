package vn.gastroai.be.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import vn.gastroai.be.config.GeminiProperties;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class GeminiStreamingChatClient {

    private final RestClient restClient;
    private final String model;
    private final ObjectMapper objectMapper;
    private final GeminiRetryTemplate retryTemplate;

    public GeminiStreamingChatClient(
            RestClient geminiRestClient,
            GeminiProperties properties,
            GeminiRetryTemplate retryTemplate
    ) {
        this.restClient = geminiRestClient;
        this.model = properties.chatModel();
        this.objectMapper = new ObjectMapper();
        this.retryTemplate = retryTemplate;
    }

    public void generateStream(
            String systemPrompt,
            String userPrompt,
            Consumer<String> onToken
    ) {
        generateStream(systemPrompt, userPrompt, List.of(), onToken);
    }

    public void generateStream(
            String systemPrompt,
            String userPrompt,
            List<ImagePart> images,
            Consumer<String> onToken
    ) {
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", userPrompt));
        for (ImagePart image : images) {
            parts.add(Map.of("inlineData", Map.of(
                    "mimeType", image.mimeType(),
                    "data", image.base64Data())));
        }

        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of(
                        "parts", List.of(
                                Map.of("text", systemPrompt)
                        )
                ),
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", parts
                        )
                )
        );

        retryTemplate.withRetry(() -> restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/models/{model}:streamGenerateContent")
                        .queryParam("alt", "sse")
                        .build(model))
                .body(body)
                .exchange((request, response) -> {

                    // Nem RestClientResponseException (khong phai IllegalStateException) de
                    // GeminiRetryTemplate nhan dien duoc ma 503 va retry, dong thoi
                    // GlobalExceptionHandler.handleAiServiceFailure cung bat duoc giong het
                    // luong khong-streaming (truoc day loi nay bi handleIllegalState() bat
                    // nham thanh 409 chung chung, khong co log/thong bao rieng cho Gemini).
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw HttpServerErrorException.create(
                                response.getStatusCode(),
                                "Gemini streaming that bai",
                                response.getHeaders(),
                                new byte[0],
                                null
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
                }));
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