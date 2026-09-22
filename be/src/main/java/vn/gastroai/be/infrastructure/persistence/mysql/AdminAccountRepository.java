package vn.gastroai.be.infrastructure.persistence.mysql;
import org.springframework.data.jpa.repository.JpaRepository; import vn.gastroai.be.domain.admin.AdminAccount; import java.util.*;
public interface AdminAccountRepository extends JpaRepository<AdminAccount,Long>{ Optional<AdminAccount> findByEmail(String email); }
