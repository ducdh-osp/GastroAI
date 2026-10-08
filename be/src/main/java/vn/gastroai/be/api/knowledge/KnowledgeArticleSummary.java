package vn.gastroai.be.api.knowledge;

import vn.gastroai.be.domain.knowledge.KnowledgeArticle;

public record KnowledgeArticleSummary(
        String slug,
        String title,
        String summary,
        String category,
        Integer readingMinutes) {

    public static KnowledgeArticleSummary from(KnowledgeArticle article) {
        return new KnowledgeArticleSummary(article.getSlug(), article.getTitle(), article.getSummary(),
                article.getCategory(), article.getReadingMinutes());
    }
}
