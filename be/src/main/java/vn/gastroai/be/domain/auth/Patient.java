package vn.gastroai.be.domain.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Tài khoản Bệnh nhân — bảng patients trên PostgreSQL. Có CHECK constraint role='PATIENT'
 * ở DB (migration V6) vì trước đây bảng này từng bị dùng chung cho cả Admin/Bác sĩ, giờ
 * đã tách hẳn sang MySQL (Admin/Doctor) theo đúng kiến trúc đa cơ sở dữ liệu.
 */
@Entity
@Table(name = "patients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name")
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.PATIENT;

    // false cho tới khi bấm link trong email xác thực (UC0001) — AuthService.login()
    // chặn đăng nhập nếu còn false, dù mật khẩu đúng.
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    // Chỉ lưu hash SHA-256 của token xác thực email, không lưu token thô.
    @Column(name = "verification_token_hash")
    private String verificationTokenHash;

    @Column(name = "verification_token_expires_at")
    private Instant verificationTokenExpiresAt;

    // Tương tự verificationTokenHash nhưng cho luồng quên mật khẩu (UC0004).
    @Column(name = "password_reset_token_hash")
    private String passwordResetTokenHash;

    @Column(name = "password_reset_token_expires_at")
    private Instant passwordResetTokenExpiresAt;

    // Tăng mỗi khi đổi/reset mật khẩu — JWT cũ mang version thấp hơn sẽ bị
    // JwtAuthenticationFilter từ chối ngay, không cần đợi hết hạn tự nhiên.
    @Column(name = "token_version", nullable = false)
    private int tokenVersion;

    // UC0008: đếm số lần sai mật khẩu liên tiếp, reset về 0 khi đăng nhập đúng hoặc
    // khi lockedUntil đã hết hạn.
    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    // null = không bị khoá. Có giá trị và còn trong tương lai = đang bị khoá tạm thời.
    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
