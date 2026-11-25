package vn.gastroai.be.infrastructure.logging;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Đăng ký RequestIdFilter + AccessLogFilter chạy TRƯỚC Spring Security, để cả request bị chặn
 * 401/403 ngay ở tầng security cũng có requestId và 1 dòng access log.
 * 2 filter KHÔNG là @Component (tạo thẳng ở đây) nên Spring Boot không tự đăng ký thêm lần 2.
 */
@Configuration
public class LoggingFilterConfig {

    // Spring Security chạy ở order -100 (SecurityProperties.DEFAULT_FILTER_ORDER); số nhỏ hơn = chạy trước.
    static final int REQUEST_ID_ORDER = -120;
    static final int ACCESS_LOG_ORDER = -110;

    @Bean
    public FilterRegistrationBean<RequestIdFilter> requestIdFilterRegistration() {
        FilterRegistrationBean<RequestIdFilter> registration = new FilterRegistrationBean<>(new RequestIdFilter());
        registration.setName("requestIdFilter");
        registration.setOrder(REQUEST_ID_ORDER);
        registration.addUrlPatterns("/*");
        // ASYNC/ERROR: log ở lần dispatch sau của chat streaming / trang lỗi vẫn mang requestId.
        registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<AccessLogFilter> accessLogFilterRegistration(
            @Value("${app.logging.slow-request-ms:2000}") long slowRequestMs) {
        FilterRegistrationBean<AccessLogFilter> registration =
                new FilterRegistrationBean<>(new AccessLogFilter(slowRequestMs));
        registration.setName("accessLogFilter");
        registration.setOrder(ACCESS_LOG_ORDER);
        registration.addUrlPatterns("/*");
        // Chỉ REQUEST: mỗi request đúng 1 dòng access log.
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        return registration;
    }
}
