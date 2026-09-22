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

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            authService.logoutSafely(authorization.substring(7));
        }
        return ResponseEntity.noContent().build();
    }

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

