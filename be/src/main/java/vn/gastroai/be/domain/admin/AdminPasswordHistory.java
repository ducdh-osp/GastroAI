package vn.gastroai.be.domain.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Mã băm của một mật khẩu CMS đã từng dùng, để chặn đặt lại mật khẩu cũ. */
@Entity
@Table(name = "admin_password_history")
@Getter
@NoArgsConstructor
public class AdminPasswordHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public AdminPasswordHistory(Admin admin, String passwordHash, Instant createdAt) {
        this.admin = admin;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }
}