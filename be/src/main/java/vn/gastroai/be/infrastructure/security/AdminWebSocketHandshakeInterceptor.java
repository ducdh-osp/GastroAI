package vn.gastroai.be.infrastructure.security;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import vn.gastroai.be.application.auth.CmsAuthService;

import java.util.Map;
import java.util.Set;

@Component
public class AdminWebSocketHandshakeInterceptor implements HandshakeInterceptor {
    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "DOCTOR");
     @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {

        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpSession session = servletRequest.getServletRequest().getSession(false);

            if (session != null) {
                Object userType = session.getAttribute(CmsAuthService.AUTH_USER_TYPE);

                if (userType != null && ALLOWED_ROLES.contains(userType.toString())) {
                    return true;
                }
            }
        }

        response.setStatusCode(HttpStatus.FORBIDDEN);
        return false;
    }
     @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // Khong can lam gi sau khi bat tay thanh cong.
    }

}
