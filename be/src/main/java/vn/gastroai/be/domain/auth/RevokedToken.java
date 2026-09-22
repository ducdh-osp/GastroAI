package vn.gastroai.be.domain.auth;
import jakarta.persistence.*; import lombok.*; import java.time.Instant;
@Entity @Table(name="revoked_tokens") @Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RevokedToken {
 @Id private String jti;
 @Column(name="expires_at", nullable=false) private Instant expiresAt;
 @Column(name="revoked_at", nullable=false) private Instant revokedAt=Instant.now();
}
