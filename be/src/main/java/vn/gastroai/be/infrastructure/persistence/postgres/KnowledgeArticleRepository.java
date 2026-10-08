package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.gastroai.be.domain.knowledge.KnowledgeArticle;

import java.util.Optional;

public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, Long> {

    @Query("""
            SELECT a FROM KnowledgeArticle a
            WHERE a.published = true
              AND (:category IS NULL OR lower(a.category) = lower(:category))
              AND (:query IS NULL OR
                   lower(a.title) LIKE lower(concat('%', :query, '%')) OR
                   lower(a.summary) LIKE lower(concat('%', :query, '%')) OR
                   lower(a.content) LIKE lower(concat('%', :query, '%')))
            ORDER BY a.sortOrder ASC, a.title ASC
            """)
    Page<KnowledgeArticle> searchPublished(
            @Param("query") String query,
            @Param("category") String category,
            Pageable pageable);

    Optional<KnowledgeArticle> findBySlugAndPublishedTrue(String slug);
}
