package vn.gastroai.be.api.cms.knowledge;

import vn.gastroai.be.domain.knowledge.KnowledgeArticle;

import java.time.Instant;

/** UC0065 - Thong tin tom tat bai viet cho bang danh sach CMS. */
public record CmsArticleSummaryResponse(
        Long id,
        String slug,
        String title,
        String summary,
        String category,
        Integer readingMinutes,
        boolean published,
        String medicalReviewer,
        Instant medicalReviewedAt,
        Integer sortOrder,
        Instant updatedAt,
        Long version
) {
    public CmsArticleSummaryResponse(
            Long id, String slug, String title, String summary, String category,
            Integer readingMinutes, boolean published, String medicalReviewer,
            Instant medicalReviewedAt, Integer sortOrder, Instant updatedAt
    ) {
        this(id, slug, title, summary, category, readingMinutes, published, medicalReviewer, medicalReviewedAt, sortOrder, updatedAt, 0L);
    }

    public static CmsArticleSummaryResponse from(KnowledgeArticle article) {
        return new CmsArticleSummaryResponse(
                article.getId(),
                article.getSlug(),
                article.getTitle(),
                article.getSummary(),
                article.getCategory(),
                article.getReadingMinutes(),
                article.isPublished(),
                article.getMedicalReviewer(),
                article.getMedicalReviewedAt(),
                article.getSortOrder(),
                article.getUpdatedAt(),
                article.getVersion()
        );
    }
}
