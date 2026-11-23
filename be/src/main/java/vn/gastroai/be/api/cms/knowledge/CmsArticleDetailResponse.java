package vn.gastroai.be.api.cms.knowledge;

import vn.gastroai.be.domain.knowledge.ArticleStatus;
import vn.gastroai.be.domain.knowledge.KnowledgeArticle;

import java.time.Instant;
import java.time.LocalDate;

/** UC0065 - Thong tin chi tiet day du bai viet cho man hinh soan thao/xem chi tiet CMS. */
public record CmsArticleDetailResponse(
        Long id,
        String slug,
        String title,
        String summary,
        String category,
        String content,
        String sourceName,
        String sourceUrl,
        LocalDate sourceCheckedAt,
        boolean published,
        ArticleStatus status,
        String medicalReviewer,
        Instant medicalReviewedAt,
        Integer sortOrder,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
    public CmsArticleDetailResponse(
            Long id,
            String slug,
            String title,
            String summary,
            String category,
            String content,
            String sourceName,
            String sourceUrl,
            LocalDate sourceCheckedAt,
            boolean published,
            String medicalReviewer,
            Instant medicalReviewedAt,
            Integer sortOrder,
            Instant createdAt,
            Instant updatedAt,
            Long version
    ) {
        this(
                id, slug, title, summary, category, content, sourceName, sourceUrl, sourceCheckedAt, published,
                published ? ArticleStatus.PUBLISHED : (medicalReviewedAt != null ? ArticleStatus.HIDDEN : ArticleStatus.DRAFT),
                medicalReviewer, medicalReviewedAt, sortOrder, createdAt, updatedAt, version
        );
    }

    public CmsArticleDetailResponse(
            Long id,
            String slug,
            String title,
            String summary,
            String category,
            String content,
            String sourceName,
            String sourceUrl,
            LocalDate sourceCheckedAt,
            boolean published,
            String medicalReviewer,
            Instant medicalReviewedAt,
            Integer sortOrder,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(id, slug, title, summary, category, content, sourceName, sourceUrl, sourceCheckedAt, published, medicalReviewer, medicalReviewedAt, sortOrder, createdAt, updatedAt, 0L);
    }

    public static CmsArticleDetailResponse from(KnowledgeArticle article) {
        ArticleStatus resolvedStatus = article.getStatus() != null
                ? article.getStatus()
                : (article.isPublished() ? ArticleStatus.PUBLISHED
                        : (article.getMedicalReviewedAt() != null ? ArticleStatus.HIDDEN : ArticleStatus.DRAFT));
        return new CmsArticleDetailResponse(
                article.getId(),
                article.getSlug(),
                article.getTitle(),
                article.getSummary(),
                article.getCategory(),
                article.getContent(),
                article.getSourceName(),
                article.getSourceUrl(),
                article.getSourceCheckedAt(),
                article.isPublished(),
                resolvedStatus,
                article.getMedicalReviewer(),
                article.getMedicalReviewedAt(),
                article.getSortOrder(),
                article.getCreatedAt(),
                article.getUpdatedAt(),
                article.getVersion()
        );
    }
}
