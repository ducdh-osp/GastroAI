package vn.gastroai.be.config;

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

import java.util.List;

/**
 * Cấu hình Spring Security dùng chung cho toàn app. CSRF tắt vì API thuần JSON, không dùng
 * cookie session cho auth (JWT ở header) nên không có rủi ro CSRF truyền thống. Whitelist
 * ("/error") ở đây chỉ mở CỔNG vào các API public — bản thân từng endpoint vẫn tự kiểm tra
 * nghiệp vụ riêng (vd sai mật khẩu, hết hạn token...).
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // IF_REQUIRED (không phải STATELESS): Bệnh nhân dùng JWT nên không cần session,
                // nhưng luồng CMS login (Admin/Bác sĩ) của Thăng dùng HttpSession thật —
                // STATELESS sẽ chặn Spring Security lưu session cho nhánh đó.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
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
                                "/api/v1/cms/auth/**",
                                "/error")
                        .permitAll()
                        .requestMatchers("/api/v1/patient/**").hasRole("PATIENT")
                        // Mọi endpoint còn lại (vd /api/v1/me/**, /api/v1/auth/change-password)
                        // bắt buộc phải có Authentication hợp lệ do JwtAuthenticationFilter set.
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
