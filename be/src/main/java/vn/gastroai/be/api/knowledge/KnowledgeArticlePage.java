package vn.gastroai.be.api.knowledge;

import org.springframework.data.domain.Page;

import java.util.List;

public record KnowledgeArticlePage(
        List<KnowledgeArticleSummary> articles,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static KnowledgeArticlePage from(Page<KnowledgeArticleSummary> result) {
        return new KnowledgeArticlePage(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }
}
