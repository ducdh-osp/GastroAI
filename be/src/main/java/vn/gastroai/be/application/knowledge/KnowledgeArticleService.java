package vn.gastroai.be.application.knowledge;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.application.support.VietnameseText;
import vn.gastroai.be.domain.knowledge.KnowledgeArticle;
import vn.gastroai.be.infrastructure.persistence.postgres.KnowledgeArticleRepository;

import java.util.List;

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
        if (query != null && query.trim().length() > 100) {
            throw new IllegalArgumentException("q khong duoc vuot qua 100 ky tu");
        }
        String normalizedQuery = query == null || query.isBlank() ? "" : VietnameseText.normalize(query.trim());
        String normalizedCategory = category == null || category.isBlank()
                ? "" : VietnameseText.normalize(category.trim());
        List<KnowledgeArticle> filtered = articleRepository.findByPublishedTrueOrderBySortOrderAscTitleAsc().stream()
                .filter(article -> normalizedCategory.isEmpty()
                        || VietnameseText.normalize(article.getCategory()).equals(normalizedCategory))
                .filter(article -> normalizedQuery.isEmpty()
                        || containsNormalized(article.getTitle(), normalizedQuery)
                        || containsNormalized(article.getSummary(), normalizedQuery)
                        || containsNormalized(article.getContent(), normalizedQuery))
                .toList();
        Pageable pageable = PageRequest.of(page, size);
        long offset = pageable.getOffset();
        if (offset >= filtered.size()) {
            return new PageImpl<>(List.of(), pageable, filtered.size());
        }
        int start = Math.toIntExact(offset);
        int end = Math.min(start + size, filtered.size());
        return new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
    }

    @Transactional(readOnly = true, transactionManager = "postgresTransactionManager")
    public List<String> listCategories() {
        return articleRepository.findPublishedCategories();
    }

    private boolean containsNormalized(String source, String normalizedQuery) {
        return VietnameseText.normalize(source).contains(normalizedQuery);
    }

    @Transactional(readOnly = true, transactionManager = "postgresTransactionManager")
    public KnowledgeArticle getBySlug(String slug) {
        return articleRepository.findBySlugAndPublishedTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay bai viet"));
    }
}
