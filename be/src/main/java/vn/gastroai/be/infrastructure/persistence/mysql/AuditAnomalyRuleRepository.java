package vn.gastroai.be.infrastructure.persistence.mysql;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.audit.AuditAnomalyRule;

import java.util.List;
import java.util.Optional;

public interface AuditAnomalyRuleRepository extends JpaRepository<AuditAnomalyRule, Long> {

    List<AuditAnomalyRule> findByEnabledTrue();

    List<AuditAnomalyRule> findAllByOrderByIdAsc();

    Optional<AuditAnomalyRule> findByCode(String code);
}