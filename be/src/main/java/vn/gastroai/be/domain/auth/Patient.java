package vn.gastroai.be.domain.auth;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Table(name="patients") @Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Patient {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true) private String email;
 @Column(name="password_hash",nullable=false) private String passwordHash;
 @Column(name="full_name") private String fullName;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private UserRole role=UserRole.PATIENT;
 @Column(name="email_verified",nullable=false) private boolean emailVerified;
 @Column(name="verification_token_hash") private String verificationTokenHash;
 @Column(name="verification_token_expires_at") private Instant verificationTokenExpiresAt;
 @Column(name="password_reset_token_hash") private String passwordResetTokenHash;
 @Column(name="password_reset_token_expires_at") private Instant passwordResetTokenExpiresAt;
 @Column(name="token_version",nullable=false) private int tokenVersion;
 @Column(name="failed_login_attempts",nullable=false) private int failedLoginAttempts;
 @Column(name="locked_until") private Instant lockedUntil;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt=Instant.now();
 @Column(name="updated_at",nullable=false) private Instant updatedAt=Instant.now();
 @PreUpdate void touch(){updatedAt=Instant.now();}
}
