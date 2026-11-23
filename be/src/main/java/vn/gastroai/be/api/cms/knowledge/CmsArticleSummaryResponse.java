package vn.gastroai.be.api.cms.knowledge;

import vn.gastroai.be.domain.knowledge.ArticleStatus;
import vn.gastroai.be.domain.knowledge.KnowledgeArticle;

import java.time.Instant;

/** UC0065 - Thong tin tom tat bai viet cho bang danh sach CMS. */
public record CmsArticleSummaryResponse(
        Long id,
        String slug,
        String title,
        String summary,
        String category,
        boolean published,
        ArticleStatus status,
        String medicalReviewer,
        Instant medicalReviewedAt,
        Integer sortOrder,
        Instant updatedAt,
        Long version
) {
    public CmsArticleSummaryResponse(
            Long id, String slug, String title, String summary, String category,
            boolean published, String medicalReviewer,
            Instant medicalReviewedAt, Integer sortOrder, Instant updatedAt, Long version
    ) {
        this(
                id, slug, title, summary, category, published,
                published ? ArticleStatus.PUBLISHED : (medicalReviewedAt != null ? ArticleStatus.HIDDEN : ArticleStatus.DRAFT),
                medicalReviewer, medicalReviewedAt, sortOrder, updatedAt, version
        );
    }

    public CmsArticleSummaryResponse(
            Long id, String slug, String title, String summary, String category,
            boolean published, String medicalReviewer,
            Instant medicalReviewedAt, Integer sortOrder, Instant updatedAt
    ) {
        this(id, slug, title, summary, category, published, medicalReviewer, medicalReviewedAt, sortOrder, updatedAt, 0L);
    }

    public static CmsArticleSummaryResponse from(KnowledgeArticle article) {
        ArticleStatus resolvedStatus = article.getStatus() != null
                ? article.getStatus()
                : (article.isPublished() ? ArticleStatus.PUBLISHED
                        : (article.getMedicalReviewedAt() != null ? ArticleStatus.HIDDEN : ArticleStatus.DRAFT));
        return new CmsArticleSummaryResponse(
                article.getId(),
                article.getSlug(),
                article.getTitle(),
                article.getSummary(),
                article.getCategory(),
                article.isPublished(),
                resolvedStatus,
                article.getMedicalReviewer(),
                article.getMedicalReviewedAt(),
                article.getSortOrder(),
                article.getUpdatedAt(),
                article.getVersion()
        );
    }
}
