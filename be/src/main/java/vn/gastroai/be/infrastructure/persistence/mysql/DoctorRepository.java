package vn.gastroai.be.infrastructure.persistence.mysql;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.gastroai.be.domain.admin.Doctor;

import java.util.Optional;

/** Dùng bởi CmsAuthService để tra cứu Bác sĩ theo email khi đăng nhập CMS. */
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByEmail(String email);

    @Query("""
            select d from Doctor d
            where lower(d.email) like lower(concat('%', :email, '%')) escape '!'
               or lower(coalesce(d.fullName, '')) like lower(concat('%', :fullName, '%')) escape '!'
            """)
    Page<Doctor> findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(
            @Param("email") String email, @Param("fullName") String fullName, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Doctor d where d.id = :id")
    Optional<Doctor> findByIdForUpdate(@Param("id") Long id);

    /**
     * Giống findByEmail nhưng khoá row (SELECT ... FOR UPDATE) - dùng riêng cho
     * CmsAuthService.login() để 2 request đăng nhập sai cùng lúc của cùng 1 tài khoản
     * không bị race condition khi cùng đọc/tăng failedLoginAttempts, giống hệt cách
     * PatientRepository.findByEmailForUpdate() đã làm.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Doctor d where d.email = :email")
    Optional<Doctor> findByEmailForUpdate(@Param("email") String email);
}
