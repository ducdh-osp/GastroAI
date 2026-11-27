package vn.gastroai.be.api.cms.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.api.auth.ChangePasswordRequest;
import vn.gastroai.be.api.auth.LoginHistoryResponse;
import vn.gastroai.be.application.audit.AuditLogService;
import vn.gastroai.be.application.auth.CmsAuthService;
import vn.gastroai.be.application.commands.CmsLoginCommand;
import vn.gastroai.be.application.readmodel.CmsAuthResult;
import vn.gastroai.be.application.readmodel.LoginHistoryItem;
import vn.gastroai.be.domain.audit.AuditAction;
import vn.gastroai.be.infrastructure.security.AdminWebSocketSessionRegistry;

/**
 * UC0045 - API tài khoản của cổng CMS: đăng nhập, đăng xuất, lịch sử đăng nhập, đổi mật khẩu.
 * Chỉ login và logout cho gọi khi chưa đăng nhập; hai API còn lại SecurityConfig yêu cầu
 * phiên CMS còn hiệu lực.
 */
@RestController
public class CmsAuthController {

    private final CmsAuthService cmsAuthService;
    private final AuditLogService auditLogService;
    private final AdminWebSocketSessionRegistry webSocketSessions;

    public CmsAuthController(CmsAuthService cmsAuthService, AuditLogService auditLogService,
            AdminWebSocketSessionRegistry webSocketSessions) {
        this.cmsAuthService = cmsAuthService;
        this.auditLogService = auditLogService;
        this.webSocketSessions = webSocketSessions;
    }

    @PostMapping("/api/v1/cms/auth/login")
    public CmsAuthResult login(@Valid @RequestBody CmsLoginRequest request, HttpServletRequest httpRequest) {
        CmsAuthResult result = cmsAuthService.login(new CmsLoginCommand(request.email(), request.password()), httpRequest);
        // Ghi vào nhật ký để quy tắc MULTI_IP phát hiện được đăng nhập từ nhiều nơi (UC0063).
        auditLogService.record(AuditAction.CMS_LOGIN, "ADMIN", result.userId(), null);
        return result;
    }

    @PostMapping("/api/v1/cms/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        cmsAuthService.logout(request);
    }

    @GetMapping("/api/v1/cms/auth/me")
    public CmsAuthResult me(HttpServletRequest request) {
        return cmsAuthService.currentAccount(currentAccountId(request.getSession(false)));
    }

    /**
     * Lịch sử đăng nhập của chính tài khoản đang đăng nhập. Mã tài khoản lấy từ phiên,
     * không nhận từ request, nên không ai xem được lịch sử của người khác.
     */
    @GetMapping("/api/v1/cms/auth/login-history")
    public LoginHistoryResponse history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Trang phải từ 0 trở lên và số dòng mỗi trang từ 1 đến 100");
        }
        Long accountId = currentAccountId(request.getSession(false));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "attemptedAt"));
        Page<LoginHistoryItem> result = cmsAuthService.loginHistory(accountId, pageable);

        return new LoginHistoryResponse(
                result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(),
                cmsAuthService.recentFailureCount(accountId));
    }

    /**
     * UC0005 - đổi mật khẩu xong vẫn ở lại trang, không bị đăng xuất.
     * Mọi phiên khác của tài khoản hết hiệu lực vì số phiên tăng; phiên hiện tại được cấp mã mới
     * (chống cố định phiên) và ghi số phiên mới nên vẫn dùng tiếp được.
     */
    @PostMapping("/api/v1/cms/auth/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest body, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Long accountId = currentAccountId(session);

        int sessionVersion = cmsAuthService.changePassword(
                accountId, body.currentPassword(), body.newPassword(), request);
        auditLogService.record(AuditAction.PASSWORD_CHANGE, "ADMIN", accountId, null);

        // Service đã commit xong mới cập nhật phiên: commit lỗi thì phiên vẫn giữ nguyên như cũ.
        request.changeSessionId();
        session.setAttribute(CmsAuthService.AUTH_SESSION_VERSION, sessionVersion);
        // Kết nối realtime mở bằng phiên cũ phải dừng; tab hiện tại tự kết nối lại bằng phiên mới.
        webSocketSessions.closeSessionsOfAccount(accountId);
    }

    private Long currentAccountId(HttpSession session) {
        Long accountId = CmsAuthService.sessionUserId(session);
        if (accountId == null) {
            throw new AccessDeniedException("Chưa đăng nhập");
        }
        return accountId;
    }
}