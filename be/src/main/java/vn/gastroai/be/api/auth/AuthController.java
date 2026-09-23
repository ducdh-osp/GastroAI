package vn.gastroai.be.api.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.application.auth.AuthService;
import vn.gastroai.be.application.auth.ClientRequestInfoResolver;
import vn.gastroai.be.application.commands.LoginCommand;

import java.util.Map;

/** REST API cho UC0001-0005 (đăng ký, xác thực email, quên/đổi/đặt lại mật khẩu, đăng nhập/xuất). */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final ClientRequestInfoResolver requestInfoResolver;

    public AuthController(AuthService authService,
                          ClientRequestInfoResolver requestInfoResolver) {
        this.authService = authService;
        this.requestInfoResolver = requestInfoResolver;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request.email(), request.password(), request.fullName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Da dang ky. Vui long kiem tra email de xac thuc."));
    }

    @PostMapping("/verify-email")
    public Map<String, String> verify(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verify(request.token());
        return Map.of("message", "Email da duoc xac thuc");
    }

    // UC0001 - Issue #7: gửi lại token xác thực mới khi token cũ hết hạn (24h) mà chưa
    // bấm link. Cùng nguyên tắc "không tiết lộ" như forgot-password bên dưới: message trả về
    // luôn chung chung dù email tồn tại/đã xác thực hay không (xem AuthService.resendVerification).
    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerification(request.email());
        return ResponseEntity.ok(Map.of(
                "message", "Neu email ton tai va chua xac thuc, chung toi da gui lai link"));
    }

    // Message trả về luôn chung chung dù email có tồn tại hay không — không cho phép
    // kẻ tấn công dò xem email nào đã đăng ký qua nội dung phản hồi.
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request.email());
        return ResponseEntity.ok(Map.of(
                "message", "Neu email ton tai, huong dan dat lai mat khau da duoc gui"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request,
                              HttpServletRequest servletRequest) {
        var result = authService.login(
                new LoginCommand(request.email(), request.password()),
                requestInfoResolver.resolve(servletRequest));
        return AuthResponse.from(result);
    }

    // Endpoint permitAll (xem SecurityConfig) — không bắt buộc token hợp lệ, vì logout với
    // token đã hết hạn/hỏng vẫn phải trả về thành công (client chỉ cần xoá token phía mình).
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            authService.logoutSafely(authorization.substring(7));
        }
        return ResponseEntity.noContent().build();
    }

    // principal.getName() lấy từ subject của JWT (chính là patientId) — do
    // JwtAuthenticationFilter set vào SecurityContext, endpoint này bắt buộc đã đăng nhập.
    @PostMapping("/change-password")
    public ResponseEntity<Void> change(@Valid @RequestBody ChangePasswordRequest request,
                                       java.security.Principal principal) {
        authService.changePassword(
                Long.valueOf(principal.getName()),
                request.currentPassword(),
                request.newPassword());
        return ResponseEntity.noContent().build();
    }
}

