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
import vn.gastroai.be.domain.admin.Admin;
import vn.gastroai.be.domain.admin.CmsRole;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminRepository;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * UC0067 - Chỉ Admin và Giám sát vận hành (SCR-AD-13: cùng xử lý cảnh báo khẩn cấp) có phiên
 * còn hiệu lực mới mở được kết nối realtime /ws. Kiểm tra giống AdminSessionFilter: vai trò và
 * số phiên đọc lại từ cơ sở dữ liệu, nên phiên cũ sau khi đổi mật khẩu không mở được kết nối mới.
 * Vai trò thật được lưu vào session attribute để StompClientSendBlockingInterceptor chỉ cho
 * Admin đăng ký kênh thông báo Admin; Giám sát vận hành chỉ nhận các kênh cảnh báo Triage.
 */
@Component
public class AdminWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    public static final String HTTP_SESSION_ID_ATTRIBUTE = "HTTP_SESSION_ID";
    public static final String CMS_USER_ID_ATTRIBUTE = "CMS_USER_ID";
    public static final String CMS_USER_TYPE_ATTRIBUTE = "CMS_USER_TYPE";

    private static final Set<CmsRole> ALLOWED_ROLES = EnumSet.of(CmsRole.ADMIN, CmsRole.OPERATIONS);

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

            Optional<CmsRole> role = accountId == null ? Optional.empty() : activeRole(accountId, session);
            if (role.isPresent()) {
                attributes.put(HTTP_SESSION_ID_ATTRIBUTE, session.getId());
                attributes.put(CMS_USER_ID_ATTRIBUTE, accountId);
                attributes.put(CMS_USER_TYPE_ATTRIBUTE, role.get().name());
                return true;
            }
        }

        response.setStatusCode(HttpStatus.FORBIDDEN);
        return false;
    }

    private Optional<CmsRole> activeRole(Long accountId, HttpSession session) {
        AdminRepository repository = admins.getIfAvailable();
        if (repository == null) {
            return Optional.empty();
        }
        return repository.findById(accountId)
                .filter(account -> ALLOWED_ROLES.contains(account.getRole()))
                .filter(account -> CmsAuthService.isCurrentSession(account, session))
                .map(Admin::getRole);
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