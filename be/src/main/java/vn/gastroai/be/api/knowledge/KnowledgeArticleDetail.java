package vn.gastroai.be.api.knowledge;

import vn.gastroai.be.domain.knowledge.KnowledgeArticle;

import java.time.Instant;
import java.time.LocalDate;

public record KnowledgeArticleDetail(
        String slug,
        String title,
        String summary,
        String category,
        String content,
        String sourceName,
        String sourceUrl,
        LocalDate sourceCheckedAt,
        int readingMinutes,
        Instant updatedAt) {

    public static KnowledgeArticleDetail from(KnowledgeArticle article) {
        return new KnowledgeArticleDetail(article.getSlug(), article.getTitle(), article.getSummary(),
                article.getCategory(), article.getContent(),
                article.getSourceName(), article.getSourceUrl(), article.getSourceCheckedAt(),
                KnowledgeArticleSummary.estimateReadingMinutes(article.getContent()),
                article.getUpdatedAt());
    }
}
