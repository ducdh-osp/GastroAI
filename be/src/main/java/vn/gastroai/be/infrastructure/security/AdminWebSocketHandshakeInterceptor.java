package vn.gastroai.be.infrastructure.security;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import vn.gastroai.be.application.auth.CmsAuthService;
import vn.gastroai.be.domain.admin.CmsRole;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminRepository;

import java.util.Map;

/**
 * UC0067 - Chỉ Admin có phiên còn hiệu lực mới mở được kết nối realtime /ws.
 * Kiểm tra giống AdminSessionFilter: vai trò và số phiên đọc lại từ cơ sở dữ liệu,
 * nên phiên cũ sau khi đổi mật khẩu không mở được kết nối mới.
 */
@Component
public class AdminWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    public static final String HTTP_SESSION_ID_ATTRIBUTE = "HTTP_SESSION_ID";
    public static final String CMS_USER_ID_ATTRIBUTE = "CMS_USER_ID";
    public static final String CMS_USER_TYPE_ATTRIBUTE = "CMS_USER_TYPE";

    private final ObjectProvider<AdminRepository> admins;

    public AdminWebSocketHandshakeInterceptor(ObjectProvider<AdminRepository> admins) {
        this.admins = admins;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {

        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpSession session = servletRequest.getServletRequest().getSession(false);
            Long accountId = CmsAuthService.sessionUserId(session);

            if (accountId != null && isActiveAdmin(accountId, session)) {
                attributes.put(HTTP_SESSION_ID_ATTRIBUTE, session.getId());
                attributes.put(CMS_USER_ID_ATTRIBUTE, accountId);
                attributes.put(CMS_USER_TYPE_ATTRIBUTE, CmsRole.ADMIN.name());
                return true;
            }
        }

        response.setStatusCode(HttpStatus.FORBIDDEN);
        return false;
    }

    private boolean isActiveAdmin(Long accountId, HttpSession session) {
        AdminRepository repository = admins.getIfAvailable();
        if (repository == null) {
            return false;
        }
        return repository.findById(accountId)
                .filter(account -> account.getRole() == CmsRole.ADMIN)
                .filter(account -> CmsAuthService.isCurrentSession(account, session))
                .isPresent();
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // Không cần làm gì sau khi bắt tay thành công.
    }
}