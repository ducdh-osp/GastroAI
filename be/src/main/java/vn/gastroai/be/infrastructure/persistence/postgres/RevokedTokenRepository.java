package vn.gastroai.be.infrastructure.persistence.postgres;
import org.springframework.data.jpa.repository.JpaRepository; import vn.gastroai.be.domain.auth.RevokedToken;
public interface RevokedTokenRepository extends JpaRepository<RevokedToken,String>{}
