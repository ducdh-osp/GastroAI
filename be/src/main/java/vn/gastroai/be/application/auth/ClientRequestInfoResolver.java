package vn.gastroai.be.application.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ClientRequestInfoResolver {
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
