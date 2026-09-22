package vn.gastroai.be.api.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.api.auth.ChangePasswordRequest;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminAccountRepository;


@RestController
@RequestMapping("/api/v1/auth/admin")
public class AdminAuthController {
    private final AdminAccountRepository repository;
    private final PasswordEncoder passwordEncoder;

    public AdminAuthController(AdminAccountRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        return ResponseEntity.noContent().build();
    }

    @Transactional("mysqlTransactionManager")
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest servletRequest) {
        HttpSession session = servletRequest.getSession(false);
        if (session == null || session.getAttribute("ADMIN_ID") == null) {
            throw new BadCredentialsException("Phien admin khong hop le");
        }
        Long id = Long.valueOf(session.getAttribute("ADMIN_ID").toString());
        var admin = repository.findById(id)
                .orElseThrow(() -> new BadCredentialsException("Tai khoan khong ton tai"));
        if (!passwordEncoder.matches(request.currentPassword(), admin.getPasswordHash())) {
            throw new BadCredentialsException("Mat khau hien tai khong dung");
        }
        if (passwordEncoder.matches(request.newPassword(), admin.getPasswordHash())) {
            throw new IllegalArgumentException("Mat khau moi phai khac mat khau hien tai");
        }
        admin.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        session.invalidate();
        return ResponseEntity.noContent().build();
    }
}

