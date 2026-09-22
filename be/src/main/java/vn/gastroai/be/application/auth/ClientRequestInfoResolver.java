package vn.gastroai.be.application.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class ClientRequestInfoResolver {
    private final Set<String> trustedProxies;
    public ClientRequestInfoResolver(@Value("${app.security.trusted-proxies:}") String configured) {
        trustedProxies = Arrays.stream(configured.split(","))
                .map(String::trim).filter(s -> !s.isBlank()).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
    public ClientRequestInfo resolve(HttpServletRequest request) {
        String remote = Optional.ofNullable(request.getRemoteAddr()).orElse("unknown");
        String ip = remote;
        if (trustedProxies.contains(remote)) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) ip = forwarded.split(",")[0].trim();
        }
        if (ip.length() > 45) ip = ip.substring(0, 45);
        String ua = Optional.ofNullable(request.getHeader("User-Agent")).filter(s -> !s.isBlank()).orElse("unknown");
        if (ua.length() > 1024) ua = ua.substring(0, 1024);
        return new ClientRequestInfo(ip, ua, deviceLabel(ua));
    }
    private String deviceLabel(String ua) {
        String browser = ua.contains("Edg/") ? "Edge" : ua.contains("Chrome/") ? "Chrome" : ua.contains("Firefox/") ? "Firefox" : ua.contains("Safari/") ? "Safari" : "Trình duyệt khác";
        String os = ua.contains("Windows") ? "Windows" : ua.contains("Mac OS") ? "macOS" : ua.contains("Android") ? "Android" : ua.contains("iPhone") ? "iPhone" : ua.contains("Linux") ? "Linux" : "Thiết bị khác";
        return browser + " trên " + os;
    }
}
