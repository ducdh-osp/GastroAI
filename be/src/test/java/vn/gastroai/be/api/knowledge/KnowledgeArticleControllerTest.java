package vn.gastroai.be.api.knowledge;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import vn.gastroai.be.application.knowledge.KnowledgeArticleService;
import vn.gastroai.be.domain.knowledge.KnowledgeArticle;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeArticleControllerTest {

    private final KnowledgeArticleService service = mock(KnowledgeArticleService.class);
    private final KnowledgeArticleController controller = new KnowledgeArticleController(service);

    @Test
    void listReturnsSummariesAndPagination() {
        KnowledgeArticle article = article("trao-nguoc", "Trào ngược dạ dày");
        when(service.list("trao nguoc", null, 0, 12))
                .thenReturn(new PageImpl<>(List.of(article), PageRequest.of(0, 12), 1));

        KnowledgeArticlePage result = controller.list("trao nguoc", null, 0, 12);

        assertEquals(1, result.totalElements());
        assertEquals("trao-nguoc", result.articles().getFirst().slug());
        assertEquals("Trào ngược dạ dày", result.articles().getFirst().category());
    }

    @Test
    void categoriesReturnsOnlyServiceCategories() {
        when(service.listCategories()).thenReturn(List.of("Dạ dày", "Đại tràng và ruột"));

        assertEquals(List.of("Dạ dày", "Đại tràng và ruột"), controller.categories());
        verify(service).listCategories();
    }

    @Test
    void detailLooksUpTheRequestedSlug() {
        when(service.getBySlug("trao-nguoc")).thenReturn(article("trao-nguoc", "Dạ dày"));

        assertEquals("trao-nguoc", controller.detail("trao-nguoc").slug());
        verify(service).getBySlug("trao-nguoc");
    }

    private KnowledgeArticle article(String slug, String category) {
        KnowledgeArticle article = new KnowledgeArticle();
        article.setSlug(slug);
        article.setTitle("Bài thử");
        article.setSummary("Tóm tắt");
        article.setCategory(category);
        article.setReadingMinutes(3);
        return article;
    }
}
