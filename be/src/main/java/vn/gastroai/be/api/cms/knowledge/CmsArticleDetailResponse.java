package vn.gastroai.be.api.cms.knowledge;

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
        Integer readingMinutes,
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
    public static CmsArticleDetailResponse from(KnowledgeArticle article) {
        return new CmsArticleDetailResponse(
                article.getId(),
                article.getSlug(),
                article.getTitle(),
                article.getSummary(),
                article.getCategory(),
                article.getReadingMinutes(),
                article.getContent(),
                article.getSourceName(),
                article.getSourceUrl(),
                article.getSourceCheckedAt(),
                article.isPublished(),
                article.getMedicalReviewer(),
                article.getMedicalReviewedAt(),
                article.getSortOrder(),
                article.getCreatedAt(),
                article.getUpdatedAt()
        );
    }
}
