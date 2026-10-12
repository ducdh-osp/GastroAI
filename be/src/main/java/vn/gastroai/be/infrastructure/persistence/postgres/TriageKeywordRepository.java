package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.triage.TriageKeyword;
import vn.gastroai.be.domain.triage.TriageKeywordGroup;

import java.util.List;

public interface TriageKeywordRepository extends JpaRepository<TriageKeyword, Long> {
    List<TriageKeyword> findAllByOrderByGroupCodeAscKeywordAsc();

    boolean existsByGroupCodeAndKeyword(TriageKeywordGroup groupCode, String keyword);

    boolean existsByGroupCodeAndKeywordAndIdNot(TriageKeywordGroup groupCode, String keyword, Long id);
}
