package vn.gastroai.be.api.auth;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.application.auth.AuthService;
import vn.gastroai.be.application.readmodel.LoginHistoryItem;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {
    private final AuthService authService;

    public MeController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login-history")
    public LoginHistoryResponse history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            java.security.Principal principal,
            Authentication authentication) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page >= 0 va size trong khoang 1..100");
        }
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException("Chua dang nhap");
        }
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "attemptedAt"));
        Page<LoginHistoryItem> result = authService.history(
                Long.valueOf(principal.getName()), pageable)
                .map(history -> new LoginHistoryItem(
                        history.getId(), history.getAttemptedAt(), history.getOutcome().name(),
                        history.getFailureReason(), history.getIpAddress(),
                        history.getUserAgent(), history.getDeviceLabel()));
        return response(result);
    }

    private LoginHistoryResponse response(Page<LoginHistoryItem> page) {
        return new LoginHistoryResponse(
                page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
