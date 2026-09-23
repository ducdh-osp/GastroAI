package vn.gastroai.be.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** UC0028/029/033 - đọc từ app.gemini.* trong application.yml, xem chú thích ở đó. */
@ConfigurationProperties(prefix = "app.gemini")
public record GeminiProperties(
        String apiKey,
        String baseUrl,
        String embeddingModel,
        String chatModel
) {
}
