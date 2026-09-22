package vn.gastroai.be.infrastructure.persistence.mysql;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.admin.Admin;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByEmail(String email);
}