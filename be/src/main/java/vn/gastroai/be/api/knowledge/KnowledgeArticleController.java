package vn.gastroai.be.api.knowledge;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.application.knowledge.KnowledgeArticleService;
import vn.gastroai.be.domain.knowledge.KnowledgeArticle;

@RestController
@RequestMapping("/api/v1/articles")
public class KnowledgeArticleController {

    private final KnowledgeArticleService articleService;

    public KnowledgeArticleController(KnowledgeArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping
    public KnowledgeArticlePage list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Page<KnowledgeArticle> result = articleService.list(q, category, page, size);
        return KnowledgeArticlePage.from(result.map(KnowledgeArticleSummary::from));
    }

    @GetMapping("/{slug}")
    public KnowledgeArticleDetail detail(@PathVariable String slug) {
        return KnowledgeArticleDetail.from(articleService.getBySlug(slug));
    }
}
