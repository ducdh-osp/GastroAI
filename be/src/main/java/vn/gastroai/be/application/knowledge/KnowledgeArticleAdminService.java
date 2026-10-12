package vn.gastroai.be.application.knowledge;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.api.cms.knowledge.CmsArticleDetailResponse;
import vn.gastroai.be.api.cms.knowledge.CmsArticlePublishRequest;
import vn.gastroai.be.api.cms.knowledge.CmsArticleRequest;
import vn.gastroai.be.api.cms.knowledge.CmsArticleSummaryResponse;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.application.support.VietnameseText;
import vn.gastroai.be.domain.knowledge.KnowledgeArticle;
import vn.gastroai.be.infrastructure.persistence.postgres.KnowledgeArticleRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** UC0065 - Quan ly bai viet kien thuc tieu hoa (CRUD, danh muc, duyet y khoa) cho Admin/Bac si. */
@Service
public class KnowledgeArticleAdminService {

    private static final String TX = "postgresTransactionManager";
    private static final int SORT_STEP = 10;
    private static final int WORDS_PER_MINUTE = 200;

    private final KnowledgeArticleRepository articleRepository;

    public KnowledgeArticleAdminService(KnowledgeArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    @Transactional(value = TX, readOnly = true)
    public Page<CmsArticleSummaryResponse> list(String query, String category, Boolean published, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page >= 0 va size trong khoang 1..100");
        }
        String normalizedQuery = query == null || query.isBlank() ? "" : VietnameseText.normalize(query.trim());
        String normalizedCategory = category == null || category.isBlank() ? "" : VietnameseText.normalize(category.trim());

        List<KnowledgeArticle> filtered = articleRepository.findAllByOrderBySortOrderAscTitleAsc().stream()
                .filter(a -> published == null || a.isPublished() == published)
                .filter(a -> normalizedCategory.isEmpty()
                        || VietnameseText.normalize(a.getCategory()).equals(normalizedCategory))
                .filter(a -> normalizedQuery.isEmpty()
                        || containsNormalized(a.getTitle(), normalizedQuery)
                        || containsNormalized(a.getSummary(), normalizedQuery)
                        || containsNormalized(a.getContent(), normalizedQuery))
                .toList();

        Pageable pageable = PageRequest.of(page, size);
        long offset = pageable.getOffset();
        if (offset >= filtered.size()) {
            return new PageImpl<>(List.of(), pageable, filtered.size());
        }
        int start = Math.toIntExact(offset);
        int end = Math.min(start + size, filtered.size());
        List<CmsArticleSummaryResponse> paged = filtered.subList(start, end).stream()
                .map(CmsArticleSummaryResponse::from)
                .toList();
        return new PageImpl<>(paged, pageable, filtered.size());
    }

    @Transactional(value = TX, readOnly = true)
    public CmsArticleDetailResponse getById(Long id) {
        return CmsArticleDetailResponse.from(load(id));
    }

    @Transactional(TX)
    public CmsArticleDetailResponse create(CmsArticleRequest request) {
        String title = clean(request.title(), "Tieu de khong duoc de trong");
        String summary = clean(request.summary(), "Tom tat khong duoc de trong");
        String category = clean(request.category(), "Chu de khong duoc de trong");
        String content = clean(request.content(), "Noi dung khong duoc de trong");
        String sourceName = clean(request.sourceName(), "Nguon khong duoc de trong");
        String sourceUrl = clean(request.sourceUrl(), "Duong dan nguon khong duoc de trong");

        String slug = resolveNewSlug(request.slug(), title);
        int readingMinutes = resolveReadingMinutes(request.readingMinutes(), content);
        LocalDate sourceCheckedAt = request.sourceCheckedAt() != null ? request.sourceCheckedAt() : LocalDate.now();
        boolean published = Boolean.TRUE.equals(request.published());
        int sortOrder = request.sortOrder() != null ? request.sortOrder() : nextSortOrder();

        KnowledgeArticle article = new KnowledgeArticle();
        article.setTitle(title);
        article.setSlug(slug);
        article.setSummary(summary);
        article.setCategory(category);
        article.setContent(content);
        article.setReadingMinutes(readingMinutes);
        article.setSourceName(sourceName);
        article.setSourceUrl(sourceUrl);
        article.setSourceCheckedAt(sourceCheckedAt);
        article.setPublished(published);
        article.setSortOrder(sortOrder);
        article.setCreatedAt(Instant.now());
        article.setUpdatedAt(Instant.now());

        if (request.medicalReviewer() != null && !request.medicalReviewer().isBlank()) {
            article.setMedicalReviewer(request.medicalReviewer().trim());
            article.setMedicalReviewedAt(Instant.now());
        }

        KnowledgeArticle saved = articleRepository.save(article);
        return CmsArticleDetailResponse.from(saved);
    }

    @Transactional(TX)
    public CmsArticleDetailResponse update(Long id, CmsArticleRequest request) {
        KnowledgeArticle article = load(id);

        String title = clean(request.title(), "Tieu de khong duoc de trong");
        String summary = clean(request.summary(), "Tom tat khong duoc de trong");
        String category = clean(request.category(), "Chu de khong duoc de trong");
        String content = clean(request.content(), "Noi dung khong duoc de trong");
        String sourceName = clean(request.sourceName(), "Nguon khong duoc de trong");
        String sourceUrl = clean(request.sourceUrl(), "Duong dan nguon khong duoc de trong");

        String newSlug = resolveUpdatedSlug(request.slug(), title, article);
        int readingMinutes = resolveReadingMinutes(request.readingMinutes(), content);

        article.setTitle(title);
        article.setSlug(newSlug);
        article.setSummary(summary);
        article.setCategory(category);
        article.setContent(content);
        article.setReadingMinutes(readingMinutes);
        article.setSourceName(sourceName);
        article.setSourceUrl(sourceUrl);
        if (request.sourceCheckedAt() != null) {
            article.setSourceCheckedAt(request.sourceCheckedAt());
        }
        if (request.published() != null) {
            article.setPublished(request.published());
        }
        if (request.sortOrder() != null) {
            article.setSortOrder(request.sortOrder());
        }
        if (request.medicalReviewer() != null && !request.medicalReviewer().isBlank()) {
            article.setMedicalReviewer(request.medicalReviewer().trim());
            if (article.getMedicalReviewedAt() == null) {
                article.setMedicalReviewedAt(Instant.now());
            }
        }
        article.setUpdatedAt(Instant.now());

        KnowledgeArticle saved = articleRepository.save(article);
        return CmsArticleDetailResponse.from(saved);
    }

    @Transactional(TX)
    public CmsArticleDetailResponse setPublished(Long id, CmsArticlePublishRequest request) {
        KnowledgeArticle article = load(id);
        article.setPublished(request.published());
        if (request.published() && request.medicalReviewer() != null && !request.medicalReviewer().isBlank()) {
            article.setMedicalReviewer(request.medicalReviewer().trim());
            article.setMedicalReviewedAt(Instant.now());
        }
        article.setUpdatedAt(Instant.now());
        KnowledgeArticle saved = articleRepository.save(article);
        return CmsArticleDetailResponse.from(saved);
    }

    @Transactional(TX)
    public void delete(Long id) {
        KnowledgeArticle article = load(id);
        articleRepository.delete(article);
    }

    private KnowledgeArticle load(Long id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay bai viet"));
    }

    private String resolveNewSlug(String requestedSlug, String title) {
        if (requestedSlug != null && !requestedSlug.isBlank()) {
            String slug = toSlug(requestedSlug);
            if (articleRepository.existsBySlug(slug)) {
                throw new IllegalStateException("Duong dan slug \"" + slug + "\" da ton tai");
            }
            return slug;
        }
        String base = toSlug(title);
        String candidate = base;
        int counter = 1;
        while (articleRepository.existsBySlug(candidate)) {
            candidate = base + "-" + counter++;
        }
        return candidate;
    }

    private String resolveUpdatedSlug(String requestedSlug, String title, KnowledgeArticle article) {
        String slug = (requestedSlug != null && !requestedSlug.isBlank()) ? toSlug(requestedSlug) : toSlug(title);
        if (articleRepository.existsBySlugAndIdNot(slug, article.getId())) {
            throw new IllegalStateException("Duong dan slug \"" + slug + "\" da ton tai");
        }
        return slug;
    }

    static String toSlug(String text) {
        if (text == null || text.isBlank()) return "bai-viet";
        String normalized = VietnameseText.normalize(text.trim());
        String slug = normalized.replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (slug.isBlank()) return "bai-viet";
        return slug.length() > 100 ? slug.substring(0, 100).replaceAll("-+$", "") : slug;
    }

    static int resolveReadingMinutes(Integer custom, String content) {
        if (custom != null && custom >= 1 && custom <= 30) {
            return custom;
        }
        String[] words = content.trim().split("\\s+");
        int est = (int) Math.ceil((double) words.length / WORDS_PER_MINUTE);
        return Math.max(1, Math.min(30, est));
    }

    private int nextSortOrder() {
        return articleRepository.findAllByOrderBySortOrderAscTitleAsc().stream()
                .mapToInt(KnowledgeArticle::getSortOrder)
                .max()
                .orElse(0) + SORT_STEP;
    }

    private static String clean(String raw, String errorMsg) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(errorMsg);
        }
        return raw.trim();
    }

    private static boolean containsNormalized(String source, String normalizedQuery) {
        return VietnameseText.normalize(source).contains(normalizedQuery);
    }
}
