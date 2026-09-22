package vn.gastroai.be.infrastructure.persistence.mysql;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import vn.gastroai.be.domain.admin.AdminAccount;
import java.util.*;
public interface AdminAccountRepository extends JpaRepository<AdminAccount,Long>{
 Optional<AdminAccount> findByEmail(String email);
 Optional<AdminAccount> findByPasswordResetTokenHash(String hash);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select a from AdminAccount a where a.email = :email")
 Optional<AdminAccount> findByEmailForUpdate(@Param("email") String email);
}
