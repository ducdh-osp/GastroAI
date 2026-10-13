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
import vn.gastroai.be.domain.admin.CmsAccountType;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminRepository;
import vn.gastroai.be.infrastructure.persistence.mysql.DoctorRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Set;

@Component
public class AdminWebSocketHandshakeInterceptor implements HandshakeInterceptor {
    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "DOCTOR");

    private final ObjectProvider<AdminRepository> admins;
    private final ObjectProvider<DoctorRepository> doctors;

    public AdminWebSocketHandshakeInterceptor() {
        this.admins = null;
        this.doctors = null;
    }

    @Autowired
    public AdminWebSocketHandshakeInterceptor(
            ObjectProvider<AdminRepository> admins,
            ObjectProvider<DoctorRepository> doctors) {
        this.admins = admins;
        this.doctors = doctors;
    }

    public static final String HTTP_SESSION_ID_ATTRIBUTE = "HTTP_SESSION_ID";
    public static final String CMS_USER_ID_ATTRIBUTE = "CMS_USER_ID";
    public static final String CMS_USER_TYPE_ATTRIBUTE = "CMS_USER_TYPE";

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

                Object userId = session.getAttribute(CmsAuthService.AUTH_USER_ID);
                Object accountType = session.getAttribute(CmsAuthService.AUTH_ACCOUNT_TYPE);
                if (userType != null && ALLOWED_ROLES.contains(userType.toString())
                        && hasCurrentRole(userId, accountType, userType.toString())) {
                    attributes.put(HTTP_SESSION_ID_ATTRIBUTE, session.getId());
                    attributes.put(CMS_USER_ID_ATTRIBUTE, session.getAttribute(CmsAuthService.AUTH_USER_ID));
                    attributes.put(CMS_USER_TYPE_ATTRIBUTE, userType.toString());
                    return true;
                }
            }
        }

        response.setStatusCode(HttpStatus.FORBIDDEN);
        return false;
    }

    private boolean hasCurrentRole(Object userId, Object accountType, String legacyRole) {
        // Unit tests and pre-RBAC sessions do not have repositories/account type; retain the
        // original handshake contract for those sessions. New sessions are checked against DB.
        if (admins == null || doctors == null || accountType == null || userId == null) {
            return true;
        }
        Long id;
        try {
            id = Long.valueOf(userId.toString());
        } catch (NumberFormatException exception) {
            return false;
        }
        if (CmsAccountType.ADMIN.name().equals(accountType.toString())) {
            AdminRepository repository = admins.getIfAvailable();
            return repository != null && repository.findById(id)
                    .map(admin -> "ADMIN".equals(admin.getRole().name())).orElse(false);
        }
        if (CmsAccountType.DOCTOR.name().equals(accountType.toString())) {
            DoctorRepository repository = doctors.getIfAvailable();
            return repository != null && repository.findById(id)
                    .map(doctor -> "DOCTOR".equals(doctor.getRole().name())).orElse(false);
        }
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
