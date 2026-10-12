package vn.gastroai.be.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Boot 4's JacksonAutoConfiguration giờ cấu hình JsonMapper (Jackson 3.x) chứ không còn tự
 * tạo bean com.fasterxml.jackson.databind.ObjectMapper (Jackson 2.x) như trước — khai báo thủ
 * công ở đây cho các chỗ vẫn dùng trực tiếp ObjectMapper (ChatHistoryService, ChatHistoryController).
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
