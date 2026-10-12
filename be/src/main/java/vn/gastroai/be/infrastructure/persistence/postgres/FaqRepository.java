package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}
