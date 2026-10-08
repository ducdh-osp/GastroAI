package vn.gastroai.be.application.knowledge;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.domain.knowledge.KnowledgeArticle;
import vn.gastroai.be.infrastructure.persistence.postgres.KnowledgeArticleRepository;

@Service
public class KnowledgeArticleService {

    private final KnowledgeArticleRepository articleRepository;

    public KnowledgeArticleService(KnowledgeArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    @Transactional(readOnly = true, transactionManager = "postgresTransactionManager")
    public Page<KnowledgeArticle> list(String query, String category, int page, int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw new IllegalArgumentException("page >= 0 va size trong khoang 1..50");
        }
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        String normalizedCategory = category == null || category.isBlank() ? null : category.trim();
        return articleRepository.searchPublished(normalizedQuery, normalizedCategory, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true, transactionManager = "postgresTransactionManager")
    public KnowledgeArticle getBySlug(String slug) {
        return articleRepository.findBySlugAndPublishedTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay bai viet"));
    }
}
