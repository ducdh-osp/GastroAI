package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.gastroai.be.domain.knowledge.Faq;

import java.util.List;

public interface FaqRepository extends JpaRepository<Faq, Long> {

    List<Faq> findAllByOrderBySortOrderAscIdAsc();

    List<Faq> findByPublishedTrueOrderBySortOrderAscIdAsc();

    List<Faq> findByPublishedTrueAndCategoryOrderBySortOrderAscIdAsc(String category);

    @Query("SELECT DISTINCT f.category FROM Faq f WHERE f.published = true ORDER BY f.category")
    List<String> findDistinctPublishedCategories();

    @Query("SELECT DISTINCT f.category FROM Faq f ORDER BY f.category")
    List<String> findDistinctAllCategories();

    @Query("SELECT f.category, COUNT(f) FROM Faq f GROUP BY f.category")
    List<Object[]> countByCategoryGrouped();

    long countByCategory(String category);

    @Modifying
    @Query("UPDATE Faq f SET f.category = :newName WHERE f.category = :oldName")
    int renameCategory(@Param("oldName") String oldName, @Param("newName") String newName);
}
