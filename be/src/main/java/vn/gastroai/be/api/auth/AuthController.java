package vn.gastroai.be.api.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;
import vn.gastroai.be.application.auth.*;
import vn.gastroai.be.application.commands.LoginCommand;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final AdminLoginService adminLoginService;
    private final ClientRequestInfoResolver requestInfoResolver;

    public AuthController(AuthService authService, AdminLoginService adminLoginService,
                          ClientRequestInfoResolver requestInfoResolver) {
        this.authService = authService;
        this.adminLoginService = adminLoginService;
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

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        if (!adminLoginService.requestPasswordReset(request.email())) {
            authService.requestPasswordReset(request.email());
        }
        return ResponseEntity.ok(Map.of(
                "message", "Neu email ton tai, huong dan dat lai mat khau da duoc gui"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            adminLoginService.resetPassword(request.token(), request.newPassword());
        } catch (InvalidPasswordResetTokenException notAnAdminToken) {
            authService.resetPassword(request.token(), request.newPassword());
        }
        return ResponseEntity.noContent().build();
    }

    /** Patient-only login. Staff accounts cannot use this endpoint. */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        ClientRequestInfo requestInfo = requestInfoResolver.resolve(servletRequest);
        try {
            var result = authService.login(
                    new LoginCommand(request.email(), request.password()), requestInfo, false);
            return AuthResponse.from(result);
        } catch (BadCredentialsException credentialsException) {
            // Admin nằm ở MySQL nên AuthService (PostgreSQL) không thể tự ghi nhận
            // trường hợp Admin dùng nhầm cổng bệnh nhân.
            adminLoginService.recordWrongPortalIfPresent(
                    request.email(), request.password(), requestInfo);
            throw credentialsException;
        }
    }

    /** Shared staff login screen: accepts DOCTOR JWT or ADMIN session. */
    @PostMapping("/staff/login")
    public AuthResponse staffLogin(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        ClientRequestInfo requestInfo = requestInfoResolver.resolve(servletRequest);
        try {
            var result = authService.login(
                    new LoginCommand(request.email(), request.password()), requestInfo, true);
            return AuthResponse.from(result);
        } catch (BadCredentialsException doctorCredentials) {
            var admin = adminLoginService.login(request.email(), request.password(), requestInfo);
            HttpSession session = servletRequest.getSession(true);
            servletRequest.changeSessionId();
            session.setAttribute("ADMIN_ID", admin.getId());
            session.setAttribute("ADMIN_EMAIL", admin.getEmail());
            return new AuthResponse(null, null, admin.getEmail(), admin.getFullName(), "ADMIN", null);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            authService.logoutSafely(authorization.substring(7));
        }
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("ADMIN_ID") != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> change(@Valid @RequestBody ChangePasswordRequest request,
                                       java.security.Principal principal) {
        authService.changePassword(Long.valueOf(principal.getName()), request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}

