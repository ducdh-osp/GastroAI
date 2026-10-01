package vn.gastroai.be.infrastructure.persistence.mysql;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.gastroai.be.domain.admin.Admin;

import java.util.Optional;

/** Dùng bởi CmsAuthService để tra cứu Admin theo email khi đăng nhập CMS. */
public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByEmail(String email);

    /**
     * Giống findByEmail nhưng khoá row (SELECT ... FOR UPDATE) - dùng riêng cho
     * CmsAuthService.login() để 2 request đăng nhập sai cùng lúc của cùng 1 tài khoản
     * không bị race condition khi cùng đọc/tăng failedLoginAttempts, giống hệt cách
     * PatientRepository.findByEmailForUpdate() đã làm.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Admin a where a.email = :email")
    Optional<Admin> findByEmailForUpdate(@Param("email") String email);
}