package vn.gastroai.be.infrastructure.persistence.mysql;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.gastroai.be.domain.admin.AdminPasswordHistory;

import java.util.List;

public interface AdminPasswordHistoryRepository extends JpaRepository<AdminPasswordHistory, Long> {

    @Query("select history.passwordHash from AdminPasswordHistory history where history.admin.id = :adminId")
    List<String> findPasswordHashesByAdminId(@Param("adminId") Long adminId);
}