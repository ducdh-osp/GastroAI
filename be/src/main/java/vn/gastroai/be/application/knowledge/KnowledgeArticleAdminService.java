package vn.gastroai.be.application.knowledge;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.api.cms.knowledge.CmsArticleDetailResponse;
import vn.gastroai.be.api.cms.knowledge.CmsArticlePublishRequest;
import vn.gastroai.be.api.cms.knowledge.CmsArticleRequest;
import vn.gastroai.be.api.cms.knowledge.CmsArticleSummaryResponse;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.application.support.VietnameseText;
import vn.gastroai.be.domain.knowledge.ArticleTopic;
import vn.gastroai.be.domain.knowledge.KnowledgeArticle;
import vn.gastroai.be.infrastructure.persistence.postgres.ArticleTopicRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.KnowledgeArticleRepository;

import java.net.URI;
import java.net.URISyntaxException;
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
    private final ArticleTopicRepository topicRepository;

    public KnowledgeArticleAdminService(KnowledgeArticleRepository articleRepository,
                                       ArticleTopicRepository topicRepository) {
        this.articleRepository = articleRepository;
        this.topicRepository = topicRepository;
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
        String title = clean(request.title(), "Tiêu đề không được để trống");
        String summary = clean(request.summary(), "Tóm tắt không được để trống");
        String category = validateCategory(request.category());
        String content = clean(request.content(), "Nội dung không được để trống");
        String sourceName = clean(request.sourceName(), "Tên nguồn không được để trống");
        String sourceUrl = validateSourceUrl(request.sourceUrl());

        String slug = resolveNewSlug(request.slug(), title);
        int readingMinutes = resolveReadingMinutes(request.readingMinutes(), content);
        LocalDate sourceCheckedAt = request.sourceCheckedAt() != null ? request.sourceCheckedAt() : LocalDate.now();
        if (sourceCheckedAt.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày kiểm tra nguồn không được lớn hơn thời điểm hiện tại");
        }
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
        article.setSortOrder(sortOrder);
        article.setCreatedAt(Instant.now());
        article.setUpdatedAt(Instant.now());

        if (published) {
            if (request.medicalReviewer() == null || request.medicalReviewer().isBlank()) {
                throw new IllegalArgumentException("Bài viết muốn xuất bản bắt buộc phải có thông tin bác sĩ kiểm duyệt y khoa");
            }
            article.setPublished(true);
            article.setMedicalReviewer(request.medicalReviewer().trim());
            article.setMedicalReviewedAt(Instant.now());
        } else {
            article.setPublished(false);
            if (request.medicalReviewer() != null && !request.medicalReviewer().isBlank()) {
                article.setMedicalReviewer(request.medicalReviewer().trim());
                article.setMedicalReviewedAt(Instant.now());
            }
        }

        KnowledgeArticle saved = articleRepository.save(article);
        return CmsArticleDetailResponse.from(saved);
    }

    @Transactional(TX)
    public CmsArticleDetailResponse update(Long id, CmsArticleRequest request) {
        KnowledgeArticle article = load(id);
        if (request.version() != null && !request.version().equals(article.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(KnowledgeArticle.class, id);
        }

        String title = clean(request.title(), "Tiêu đề không được để trống");
        String summary = clean(request.summary(), "Tóm tắt không được để trống");
        String category = validateCategory(request.category());
        String content = clean(request.content(), "Nội dung không được để trống");
        String sourceName = clean(request.sourceName(), "Tên nguồn không được để trống");
        String sourceUrl = validateSourceUrl(request.sourceUrl());

        String newSlug = resolveUpdatedSlug(request.slug(), title, article);
        int readingMinutes = resolveReadingMinutes(request.readingMinutes(), content);

        boolean contentChanged = !content.equals(article.getContent())
                || !title.equals(article.getTitle())
                || !summary.equals(article.getSummary());

        boolean targetPublished = request.published() != null ? request.published() : article.isPublished();

        article.setTitle(title);
        article.setSlug(newSlug);
        article.setSummary(summary);
        article.setCategory(category);
        article.setContent(content);
        article.setReadingMinutes(readingMinutes);
        article.setSourceName(sourceName);
        article.setSourceUrl(sourceUrl);
        if (request.sourceCheckedAt() != null) {
            if (request.sourceCheckedAt().isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("Ngày kiểm tra nguồn không được lớn hơn thời điểm hiện tại");
            }
            article.setSourceCheckedAt(request.sourceCheckedAt());
        }
        if (request.sortOrder() != null) {
            article.setSortOrder(request.sortOrder());
        }

        if (targetPublished) {
            if (article.isPublished() && contentChanged) {
                // Nội dung y khoa thay đổi trên bài đang xuất bản - bắt buộc phải kiểm duyệt lại
                if (request.medicalReviewer() != null && !request.medicalReviewer().isBlank()) {
                    article.setPublished(true);
                    article.setMedicalReviewer(request.medicalReviewer().trim());
                    article.setMedicalReviewedAt(Instant.now());
                } else {
                    // Tự động chuyển về bản nháp để đảm bảo an toàn y tế cho bệnh nhân
                    article.setPublished(false);
                    article.setMedicalReviewedAt(null);
                }
            } else {
                String reviewer = (request.medicalReviewer() != null && !request.medicalReviewer().isBlank())
                        ? request.medicalReviewer().trim()
                        : article.getMedicalReviewer();
                if (reviewer == null || reviewer.isBlank()) {
                    throw new IllegalArgumentException("Bài viết muốn xuất bản bắt buộc phải có thông tin bác sĩ kiểm duyệt y khoa");
                }
                article.setPublished(true);
                article.setMedicalReviewer(reviewer);
                if (article.getMedicalReviewedAt() == null || (request.medicalReviewer() != null && !request.medicalReviewer().isBlank())) {
                    article.setMedicalReviewedAt(Instant.now());
                }
            }
        } else {
            article.setPublished(false);
            if (request.medicalReviewer() != null && !request.medicalReviewer().isBlank()) {
                article.setMedicalReviewer(request.medicalReviewer().trim());
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
        if (request.version() != null && !request.version().equals(article.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(KnowledgeArticle.class, id);
        }

        if (request.published()) {
            String reviewer = (request.medicalReviewer() != null && !request.medicalReviewer().isBlank())
                    ? request.medicalReviewer().trim()
                    : article.getMedicalReviewer();
            if (reviewer == null || reviewer.isBlank()) {
                throw new IllegalArgumentException("Không thể xuất bản bài viết khi chưa có thông tin bác sĩ kiểm duyệt y khoa");
            }
            article.setPublished(true);
            article.setMedicalReviewer(reviewer);
            article.setMedicalReviewedAt(Instant.now());
        } else {
            article.setPublished(false);
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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết"));
    }

    private String validateCategory(String rawCategory) {
        String cleaned = clean(rawCategory, "Chủ đề không được để trống");
        ArticleTopic topic = topicRepository.findByNameIgnoreCase(cleaned)
                .orElseThrow(() -> new IllegalArgumentException("Chủ đề \"" + cleaned + "\" không tồn tại trong hệ thống"));
        return topic.getName();
    }

    private String validateSourceUrl(String rawUrl) {
        String url = clean(rawUrl, "Đường dẫn nguồn không được để trống");
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) || uri.getHost() == null) {
                throw new IllegalArgumentException("Đường dẫn nguồn phải là URL hợp lệ bắt đầu bằng http:// hoặc https:// và có tên miền");
            }
            return url;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Đường dẫn nguồn không hợp lệ", e);
        }
    }

    private String resolveNewSlug(String requestedSlug, String title) {
        if (requestedSlug != null && !requestedSlug.isBlank()) {
            String slug = toSlug(requestedSlug);
            if (articleRepository.existsBySlug(slug)) {
                throw new IllegalStateException("Đường dẫn slug \"" + slug + "\" đã tồn tại");
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
        if (requestedSlug != null && !requestedSlug.isBlank()) {
            String slug = toSlug(requestedSlug);
            if (articleRepository.existsBySlugAndIdNot(slug, article.getId())) {
                throw new IllegalStateException("Đường dẫn slug \"" + slug + "\" đã tồn tại");
            }
            return slug;
        }
        String base = toSlug(title);
        String candidate = base;
        int counter = 1;
        while (articleRepository.existsBySlugAndIdNot(candidate, article.getId())) {
            candidate = base + "-" + counter++;
        }
        return candidate;
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
