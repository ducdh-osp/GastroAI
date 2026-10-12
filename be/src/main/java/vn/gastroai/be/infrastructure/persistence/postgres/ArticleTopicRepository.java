package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.knowledge.ArticleTopic;

import java.util.List;

public interface ArticleTopicRepository extends JpaRepository<ArticleTopic, Long> {

    List<ArticleTopic> findAllByOrderBySortOrderAscNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
