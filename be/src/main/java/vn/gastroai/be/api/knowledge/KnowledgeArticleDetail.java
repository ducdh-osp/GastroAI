package vn.gastroai.be.api.knowledge;

import vn.gastroai.be.domain.knowledge.KnowledgeArticle;

import java.time.LocalDate;

public record KnowledgeArticleDetail(
        String slug,
        String title,
        String summary,
        String category,
        Integer readingMinutes,
        String content,
        String sourceName,
        String sourceUrl,
        LocalDate sourceCheckedAt) {

    public static KnowledgeArticleDetail from(KnowledgeArticle article) {
        return new KnowledgeArticleDetail(article.getSlug(), article.getTitle(), article.getSummary(),
                article.getCategory(), article.getReadingMinutes(), article.getContent(),
                article.getSourceName(), article.getSourceUrl(), article.getSourceCheckedAt());
    }
}
