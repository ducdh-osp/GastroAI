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
import vn.gastroai.be.api.auth.LoginHistoryResponse;
import vn.gastroai.be.application.auth.CmsAuthService;
import vn.gastroai.be.application.commands.CmsLoginCommand;
import vn.gastroai.be.application.readmodel.CmsAuthResult;
import vn.gastroai.be.application.readmodel.LoginHistoryItem;

/**
 * API đăng nhập/xuất cho cổng CMS (Admin/Bác sĩ, UC0045). Không có @RequestMapping ở class
 * (khác AuthController) — path đầy đủ khai trực tiếp ở từng @PostMapping.
 */
@RestController
public class CmsAuthController {

    private final CmsAuthService cmsAuthService;

    public CmsAuthController(CmsAuthService cmsAuthService) {
        this.cmsAuthService = cmsAuthService;
    }

    @PostMapping("/api/v1/cms/auth/login")
    public CmsAuthResult login(
            @Valid @RequestBody CmsLoginRequest request,
            HttpServletRequest httpRequest) {

        return cmsAuthService.login(
                new CmsLoginCommand(
                        request.email(),
                        request.password(),
                        request.role()),
                httpRequest);
    }

    @PostMapping("/api/v1/cms/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        cmsAuthService.logout(request);
    }

    /**
     * Lịch sử đăng nhập của Admin/Bác sĩ đang đăng nhập. Nằm dưới "/cms/auth/**" (đã permitAll
     * trong SecurityConfig) vì CMS dùng HttpSession thủ công, không qua Spring Security
     * Authentication — tự đọc session attribute AUTH_USER_ID/AUTH_USER_TYPE để xác thực,
     * giống hệt cách logout() ở trên đang làm.
     */
    @GetMapping("/api/v1/cms/auth/login-history")
    public LoginHistoryResponse history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page >= 0 va size trong khoang 1..100");
        }
        HttpSession session = request.getSession(false);
        Object userId = session == null ? null : session.getAttribute(CmsAuthService.AUTH_USER_ID);
        Object userType = session == null ? null : session.getAttribute(CmsAuthService.AUTH_USER_TYPE);
        if (userId == null || userType == null) {
            throw new AccessDeniedException("Chua dang nhap");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "attemptedAt"));
        Long id = Long.valueOf(userId.toString());
        Page<LoginHistoryItem> result = "DOCTOR".equals(userType)
                ? cmsAuthService.doctorHistory(id, pageable).map(history -> new LoginHistoryItem(
                        history.getId(), history.getAttemptedAt(), history.getOutcome().name(),
                        history.getFailureReason(), history.getIpAddress(),
                        history.getUserAgent(), history.getDeviceLabel()))
                : cmsAuthService.adminHistory(id, pageable).map(history -> new LoginHistoryItem(
                        history.getId(), history.getAttemptedAt(), history.getOutcome().name(),
                        history.getFailureReason(), history.getIpAddress(),
                        history.getUserAgent(), history.getDeviceLabel()));

        return new LoginHistoryResponse(
                result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }
}
