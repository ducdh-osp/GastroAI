package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.gastroai.be.domain.knowledge.KnowledgeArticle;

import java.util.List;
import java.util.Optional;

public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, Long> {

    List<KnowledgeArticle> findByPublishedTrueOrderBySortOrderAscTitleAsc();

    @Query("SELECT DISTINCT a.category FROM KnowledgeArticle a WHERE a.published = true ORDER BY a.category")
    List<String> findPublishedCategories();

    Optional<KnowledgeArticle> findBySlugAndPublishedTrue(String slug);
}
