package vn.gastroai.be.infrastructure.persistence.mysql;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.admin.Doctor;

import java.util.Optional;

/** Dùng bởi CmsAuthService để tra cứu Bác sĩ theo email khi đăng nhập CMS. */
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByEmail(String email);
}
