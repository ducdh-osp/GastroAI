package vn.gastroai.be.domain.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Danh sách đen JWT đã bị thu hồi (UC0003 - đăng xuất). JWT vốn stateless nên không thể
 * "xoá" — cách duy nhất để làm 1 token cụ thể hết hiệu lực trước hạn là ghi jti của nó
 * vào đây, JwtAuthenticationFilter check existsById(jti) trên mỗi request.
 * expiresAt lưu lại để biết bản ghi nào đã hết hạn tự nhiên (token hết hạn thì tự vô hiệu,
 * không cần tra bảng này nữa) — hiện chưa có job dọn định kỳ riêng cho bảng này như
 * PatientLoginHistory (xem LoginHistoryCleanupService), bảng sẽ phình dần theo số lần logout.
 */
@Entity
@Table(name = "revoked_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RevokedToken {
    // Chính là jti (JWT ID) — mỗi token có 1 id ngẫu nhiên riêng khi JwtService phát hành.
    @Id
    private String jti;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at", nullable = false)
    private Instant revokedAt = Instant.now();
}
