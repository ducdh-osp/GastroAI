package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.gastroai.be.domain.knowledge.KnowledgeArticle;

import java.util.List;
import java.util.Optional;

public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, Long> {

    List<KnowledgeArticle> findByPublishedTrueOrderBySortOrderAscTitleAsc();

    @Query("SELECT DISTINCT a.category FROM KnowledgeArticle a WHERE a.published = true ORDER BY a.category")
    List<String> findPublishedCategories();

    Optional<KnowledgeArticle> findBySlugAndPublishedTrue(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    List<KnowledgeArticle> findAllByOrderBySortOrderAscTitleAsc();

    /** UC0066 - so bai viet (ca chua xuat ban) theo tung category. */
    @Query("SELECT a.category, COUNT(a) FROM KnowledgeArticle a GROUP BY a.category")
    List<Object[]> countByCategoryGrouped();

    long countByCategory(String category);

    /** UC0066 - doi ten chu de thi cac bai viet dang dung ten cu phai doi theo. */
    @Modifying
    @Query("UPDATE KnowledgeArticle a SET a.category = :newName WHERE a.category = :oldName")
    int renameCategory(@Param("oldName") String oldName, @Param("newName") String newName);
}
