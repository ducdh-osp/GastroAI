package vn.gastroai.be.application.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * UC0007 - Trích thông tin thiết bị/IP thật của người dùng để ghi vào lịch sử đăng nhập,
 * phục vụ việc tự phát hiện truy cập lạ.
 */
@Component
public class ClientRequestInfoResolver {
    // Danh sách IP reverse proxy được tin cậy (vd Nginx). Chỉ đọc header X-Forwarded-For
    // khi request đến TỪ 1 trong các IP này — nếu đọc header đó vô điều kiện, ai cũng có
    // thể tự set X-Forwarded-For giả để giả mạo IP nguồn trong log.
    private final Set<String> trustedProxies;

    public ClientRequestInfoResolver(@Value("${app.security.trusted-proxies:}") String configured) {
        trustedProxies = Arrays.stream(configured.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    public ClientRequestInfo resolve(HttpServletRequest request) {
        String remote = Optional.ofNullable(request.getRemoteAddr()).orElse("unknown");
        String ip = remote;
        if (trustedProxies.contains(remote)) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                ip = forwarded.split(",")[0].trim();
            }
        }
        // Cắt bớt cho khớp giới hạn cột ip_address/user_agent trong migration (VARCHAR(45)/
        // VARCHAR(1024)) — tránh lỗi ghi DB nếu header bị client gửi dữ liệu bất thường/quá dài.
        if (ip.length() > 45) {
            ip = ip.substring(0, 45);
        }
        String userAgent = Optional.ofNullable(request.getHeader("User-Agent"))
                .filter(value -> !value.isBlank())
                .orElse("unknown");
        if (userAgent.length() > 1024) {
            userAgent = userAgent.substring(0, 1024);
        }
        return new ClientRequestInfo(ip, userAgent, deviceLabel(userAgent));
    }

    /** Suy ra tên trình duyệt + hệ điều hành từ User-Agent để hiển thị thân thiện ở UC0007. */
    private String deviceLabel(String userAgent) {
        String browser = userAgent.contains("Edg/") ? "Edge"
                : userAgent.contains("Chrome/") ? "Chrome"
                : userAgent.contains("Firefox/") ? "Firefox"
                : userAgent.contains("Safari/") ? "Safari"
                : "Other browser";
        String operatingSystem = userAgent.contains("Windows") ? "Windows"
                : userAgent.contains("Mac OS") ? "macOS"
                : userAgent.contains("Android") ? "Android"
                : userAgent.contains("iPhone") ? "iPhone"
                : userAgent.contains("Linux") ? "Linux"
                : "Other device";
        return browser + " on " + operatingSystem;
    }
}
