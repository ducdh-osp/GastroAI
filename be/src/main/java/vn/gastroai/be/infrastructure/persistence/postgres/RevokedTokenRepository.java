package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.auth.RevokedToken;

/** Chỉ cần existsById(jti) (JwtAuthenticationFilter) và save() (AuthService.logoutSafely). */
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, String> {
}
