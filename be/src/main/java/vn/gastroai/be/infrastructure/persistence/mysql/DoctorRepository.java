package vn.gastroai.be.infrastructure.persistence.mysql;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.admin.Doctor;

import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByEmail(String email);
}
