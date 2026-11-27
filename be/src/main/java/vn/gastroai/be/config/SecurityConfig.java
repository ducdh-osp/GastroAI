package vn.gastroai.be.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import vn.gastroai.be.infrastructure.security.JwtAuthenticationFilter;
import vn.gastroai.be.infrastructure.security.AdminSessionFilter;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import java.io.IOException;
import java.util.List;

/**
 * Cấu hình Spring Security dùng chung cho toàn app. CSRF tắt vì API thuần JSON,
 * không dùng
 * cookie session cho auth (JWT ở header) nên không có rủi ro CSRF truyền thống.
 * Whitelist
 * ("/error") ở đây chỉ mở CỔNG vào các API public — bản thân từng endpoint vẫn
 * tự kiểm tra
 * nghiệp vụ riêng (vd sai mật khẩu, hết hạn token...).
 */
@Configuration
public class SecurityConfig {
    static final String ROLE_FORBIDDEN_BODY =
            "{\"code\":\"ROLE_FORBIDDEN\",\"message\":\"Bạn không có quyền truy cập chức năng này\"}";
    static final String UNAUTHENTICATED_BODY =
            "{\"code\":\"UNAUTHENTICATED\",\"message\":\"Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại\"}";

    @Value("${app.cors.allowed-origin}")
    private String allowedOrigin;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter,
            AdminSessionFilter adminSessionFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // IF_REQUIRED (không phải STATELESS): Bệnh nhân dùng JWT nên không cần session,
                // nhưng luồng CMS login dùng HttpSession thật —
                // STATELESS sẽ chặn Spring Security lưu session cho nhánh đó.
                // sessionFixation none: không đổi mã phiên ở mỗi request (CmsAuthService.login đã tự cấp phiên mới);
                // nếu đổi, các request gửi song song bằng mã cũ sẽ bị coi là chưa đăng nhập.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fixation -> fixation.none()))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        // Các endpoint không cần đăng nhập trước: đăng ký, xác thực email,
                        // quên/đặt lại mật khẩu, login, logout (token không hợp lệ vẫn cho qua
                        // để logoutSafely tự xử lý êm), và toàn bộ cổng CMS (có luồng auth riêng).
                        .requestMatchers(
                                "/api/v1/auth/register",
                                "/api/v1/auth/verify-email",
                                "/api/v1/auth/resend-verification",
                                "/api/v1/auth/forgot-password",
                                "/api/v1/auth/reset-password",
                                "/api/v1/auth/logout",
                                "/api/v1/auth/login",
                                "/api/v1/cms/auth/login",
                                "/api/v1/cms/auth/logout",
                                "/api/v1/faqs/**",
                                "/error")
                        .permitAll()
                        // Patient endpoints must never accept a CMS HttpSession. MySQL CMS ids
                        // and PostgreSQL patient ids are independent sequences and may collide.
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/change-password").hasRole("PATIENT")
                        .requestMatchers("/api/v1/patient/**").hasRole("PATIENT")
                        .requestMatchers("/api/v1/chat/**").hasRole("PATIENT")
                        .requestMatchers(HttpMethod.GET, "/api/v1/documents/**")
                      .hasAnyRole("ADMIN", "CONTENT_EDITOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/documents/*/approve")
                        .hasRole("CONTENT_EDITOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/documents/**")
                      .hasAnyRole("ADMIN", "CONTENT_EDITOR")
                        // SockJS cannot attach the Authorization header during its HTTP handshake.
                        // This endpoint validates the JWT query token in
                        // PatientWebSocketHandshakeInterceptor.
                        .requestMatchers("/ws/patient/**").permitAll()
                        .requestMatchers("/ws/**").hasAnyRole("ADMIN", "OPERATIONS")
                        .requestMatchers("/api/v1/cms/notifications/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/cms/triage-alerts/**").hasAnyRole("ADMIN", "OPERATIONS")
                        .requestMatchers("/api/v1/cms/triage-assessments/**").hasAnyRole("ADMIN")
                        .requestMatchers("/api/v1/cms/triage-keywords/**").hasAnyRole("ADMIN", "CONTENT_EDITOR")
                        .requestMatchers("/api/v1/cms/evaluation/**").hasAnyRole("ADMIN")
                        .requestMatchers("/api/v1/cms/article-topics/**")
                        .hasAnyRole("ADMIN", "CONTENT_EDITOR", "CONTENT_AUTHOR")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/v1/cms/articles/*/publish")
                        .hasAnyRole("ADMIN", "CONTENT_EDITOR")
                        .requestMatchers("/api/v1/cms/articles/**")
                        .hasAnyRole("ADMIN", "CONTENT_EDITOR", "CONTENT_AUTHOR")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/v1/cms/faqs/*/publish")
                        .hasAnyRole("ADMIN", "CONTENT_EDITOR")
                        .requestMatchers("/api/v1/cms/faqs/**")
                        .hasAnyRole("ADMIN", "CONTENT_EDITOR", "CONTENT_AUTHOR")
                        .requestMatchers("/api/v1/cms/catalogs/**").hasAnyRole("ADMIN", "CONTENT_EDITOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/documents/**")
                        .hasAnyRole("ADMIN", "CONTENT_EDITOR")
                        .requestMatchers("/api/v1/cms/dashboard/**")
                        .hasAnyRole("ADMIN", "OPERATIONS")
                        .requestMatchers("/api/v1/cms/runtime-config/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/api/v1/cms/email-templates/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/api/v1/cms/users/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/api/v1/cms/rbac/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/documents/**")
                      .hasAnyRole("ADMIN", "CONTENT_EDITOR")
                        .requestMatchers("/api/v1/cms/auth/**")
                        .hasAnyRole("ADMIN", "CONTENT_EDITOR", "CONTENT_AUTHOR", "OPERATIONS")
                        .requestMatchers("/api/v1/cms/audit-logs/**")
                        .hasRole("ADMIN")
                        // Các endpoint còn lại phải có Authentication hợp lệ theo quyền riêng
                        // của từng nhóm API ở trên.
                        .anyRequest().authenticated())
                // Chưa đăng nhập -> 401; sai vai trò -> 403 ROLE_FORBIDDEN (khác 403 nghiệp vụ).
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler((request, response, denied) ->
                                writeJson(response, HttpServletResponse.SC_FORBIDDEN, ROLE_FORBIDDEN_BODY))
                        .authenticationEntryPoint((request, response, authException) ->
                                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, UNAUTHENTICATED_BODY)))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(adminSessionFilter, JwtAuthenticationFilter.class);
        return http.build();
    }

    private static void writeJson(HttpServletResponse response, int status, String body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(body);
    }

    private UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigin));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Content-Disposition"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
