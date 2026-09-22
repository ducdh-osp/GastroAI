package vn.gastroai.be.api.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import vn.gastroai.be.application.auth.AuthService;
import vn.gastroai.be.application.commands.LoginCommand;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminAccountRepository;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final AdminAccountRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthService authService, AdminAccountRepository adminRepository,
                          PasswordEncoder passwordEncoder) {
        this.authService = authService;
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
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

    /** Patient-only login. Staff accounts cannot use this endpoint. */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        var result = authService.login(new LoginCommand(request.email(), request.password()));
        if (!"PATIENT".equals(result.role())) {
            throw new BadCredentialsException("Tai khoan nay phai dang nhap tai cong danh cho bac si va admin");
        }
        return AuthResponse.from(result);
    }

    /** Shared staff login screen: accepts DOCTOR JWT or ADMIN session. */
    @PostMapping("/staff/login")
    public AuthResponse staffLogin(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        try {
            var result = authService.login(new LoginCommand(request.email(), request.password()));
            if (!"DOCTOR".equals(result.role())) {
                throw new BadCredentialsException("Tai khoan nay khong phai tai khoan bac si");
            }
            return AuthResponse.from(result);
        } catch (BadCredentialsException doctorCredentials) {
            var admin = adminRepository.findByEmail(request.email().trim().toLowerCase())
                    .orElseThrow(() -> doctorCredentials);
            if (!admin.isActive() || !passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
                throw doctorCredentials;
            }
            HttpSession session = servletRequest.getSession(true);
            servletRequest.changeSessionId();
            session.setAttribute("ADMIN_ID", admin.getId());
            session.setAttribute("ADMIN_EMAIL", admin.getEmail());
            return new AuthResponse(null, null, admin.getEmail(), admin.getFullName(), "ADMIN");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            authService.logout(authorization.substring(7));
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

