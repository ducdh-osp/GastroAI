package vn.gastroai.be.application.knowledge;

import org.junit.jupiter.api.Test;
import vn.gastroai.be.domain.knowledge.KnowledgeArticle;
import vn.gastroai.be.infrastructure.persistence.postgres.KnowledgeArticleRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeArticleServiceTest {

    private final KnowledgeArticleRepository repository = mock(KnowledgeArticleRepository.class);
    private final KnowledgeArticleService service = new KnowledgeArticleService(repository);

    @Test
    void searchesVietnameseWithoutDiacritics() {
        when(repository.findByPublishedTrueOrderBySortOrderAscTitleAsc())
                .thenReturn(List.of(article("trao-nguoc", "Trào ngược dạ dày", "Ợ nóng")));

        var result = service.list("trao nguoc", null, 0, 12);

        assertEquals(1, result.getTotalElements());
        assertEquals("trao-nguoc", result.getContent().getFirst().getSlug());
    }

    @Test
    void treatsLikeWildcardsAndBackslashAsLiteralText() {
        when(repository.findByPublishedTrueOrderBySortOrderAscTitleAsc())
                .thenReturn(List.of(article("literal", "Tìm % _ \\", "dữ liệu literal")));

        assertEquals(1, service.list("% _ \\", null, 0, 12).getTotalElements());
        assertEquals(1, service.list("%", null, 0, 12).getTotalElements());
        assertEquals(1, service.list("_", null, 0, 12).getTotalElements());
    }

    @Test
    void allowsNullQueryAndCategoryAndEnforcesQueryLength() {
        when(repository.findByPublishedTrueOrderBySortOrderAscTitleAsc())
                .thenReturn(List.of(article("one", "Dạ dày", "Bài"), article("two", "Đại tràng", "Bài")));

        assertEquals(2, service.list(null, null, 0, 12).getTotalElements());
        assertThrows(IllegalArgumentException.class, () -> service.list("x".repeat(101), null, 0, 12));
    }

    private KnowledgeArticle article(String slug, String title, String content) {
        KnowledgeArticle article = new KnowledgeArticle();
        article.setSlug(slug);
        article.setTitle(title);
        article.setSummary("Tóm tắt");
        article.setContent(content);
        article.setCategory("Dạ dày");
        article.setReadingMinutes(3);
        article.setSortOrder(0);
        article.setPublished(true);
        return article;
    }
}
