package vn.gastroai.be.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * UC0028/029/033 - RestClient dùng chung để gọi Gemini API (embedding + sinh câu trả lời).
 * Key truyền qua header "x-goog-api-key" (không nhét vào query string) để không lỡ lộ key
 * trong log truy cập/URL nếu có công cụ nào log lại request URI.
 */
@Configuration
public class GeminiConfig {

    @Bean
    public RestClient geminiRestClient(GeminiProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader("x-goog-api-key", properties.apiKey())
                .build();
    }
}
