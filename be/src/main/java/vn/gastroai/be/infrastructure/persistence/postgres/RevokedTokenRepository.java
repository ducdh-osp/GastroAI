package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.gastroai.be.domain.auth.RevokedToken;
import java.time.Instant;

/** Chỉ cần existsById(jti) (JwtAuthenticationFilter) và save() (AuthService.logoutSafely). */
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, String> {
    @Modifying
    @Query("delete from RevokedToken token where token.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
