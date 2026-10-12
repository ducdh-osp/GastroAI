package vn.gastroai.be.application.knowledge;

import org.junit.jupiter.api.Test;
import vn.gastroai.be.api.cms.knowledge.ArticleTopicRequest;
import vn.gastroai.be.api.cms.knowledge.ArticleTopicResponse;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.domain.knowledge.ArticleTopic;
import vn.gastroai.be.infrastructure.persistence.postgres.ArticleTopicRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.KnowledgeArticleRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleTopicServiceTest {

    private final ArticleTopicRepository topics = mock(ArticleTopicRepository.class);
    private final KnowledgeArticleRepository articles = mock(KnowledgeArticleRepository.class);
    private final ArticleTopicService service = new ArticleTopicService(topics, articles);

    private ArticleTopic topic(Long id, String name, int sortOrder) {
        ArticleTopic t = new ArticleTopic(name, sortOrder);
        t.setId(id);
        return t;
    }

    @Test
    void listIncludesArticleCountAndZeroForUnusedTopics() {
        when(topics.findAllByOrderBySortOrderAscNameAsc())
                .thenReturn(List.of(topic(1L, "Dạ dày", 10), topic(2L, "Đại tràng", 20)));
        List<Object[]> counts = new java.util.ArrayList<>();
        counts.add(new Object[]{"Dạ dày", 3L});
        when(articles.countByCategoryGrouped()).thenReturn(counts);

        List<ArticleTopicResponse> result = service.list();

        assertEquals(3L, result.get(0).articleCount());
        assertEquals(0L, result.get(1).articleCount());
    }

    @Test
    void createTrimsAndCollapsesWhitespaceAndAppendsToEnd() {
        when(topics.existsByNameIgnoreCase("Chế độ ăn")).thenReturn(false);
        when(topics.findAllByOrderBySortOrderAscNameAsc()).thenReturn(List.of(topic(1L, "Dạ dày", 30)));
        when(topics.save(any(ArticleTopic.class))).thenAnswer(inv -> {
            ArticleTopic t = inv.getArgument(0);
            t.setId(9L);
            return t;
        });

        ArticleTopicResponse result = service.create(new ArticleTopicRequest("  Chế   độ ăn ", null));

        assertEquals("Chế độ ăn", result.name());
        assertEquals(40, result.sortOrder());
    }

    @Test
    void createRejectsDuplicateNameWith409Style() {
        when(topics.existsByNameIgnoreCase("Dạ dày")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.create(new ArticleTopicRequest("Dạ dày", null)));
        verify(topics, never()).save(any());
    }

    @Test
    void createRejectsBlankName() {
        assertThrows(IllegalArgumentException.class, () -> service.create(new ArticleTopicRequest("   ", null)));
    }

    @Test
    void renameCascadesToArticles() {
        ArticleTopic existing = topic(1L, "Dạ dày", 10);
        when(topics.findById(1L)).thenReturn(Optional.of(existing));
        when(topics.existsByNameIgnoreCaseAndIdNot("Dạ dày - thực quản", 1L)).thenReturn(false);
        when(topics.save(existing)).thenReturn(existing);
        when(articles.countByCategory("Dạ dày - thực quản")).thenReturn(4L);

        ArticleTopicResponse result = service.update(1L, new ArticleTopicRequest("Dạ dày - thực quản", null));

        verify(articles).renameCategory("Dạ dày", "Dạ dày - thực quản");
        assertEquals("Dạ dày - thực quản", result.name());
        assertEquals(10, result.sortOrder());
        assertEquals(4L, result.articleCount());
    }

    @Test
    void updateWithSameNameDoesNotTouchArticles() {
        ArticleTopic existing = topic(1L, "Dạ dày", 10);
        when(topics.findById(1L)).thenReturn(Optional.of(existing));
        when(topics.save(existing)).thenReturn(existing);

        service.update(1L, new ArticleTopicRequest("Dạ dày", 50));

        verify(articles, never()).renameCategory(any(), any());
        assertEquals(50, existing.getSortOrder());
    }

    @Test
    void updateRejectsNameOfAnotherTopic() {
        when(topics.findById(1L)).thenReturn(Optional.of(topic(1L, "Dạ dày", 10)));
        when(topics.existsByNameIgnoreCaseAndIdNot("Đại tràng", 1L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.update(1L, new ArticleTopicRequest("Đại tràng", null)));
        verify(articles, never()).renameCategory(any(), any());
    }

    @Test
    void updateRejectsStaleVersionWithOptimisticLockingException() {
        ArticleTopic existing = topic(1L, "Dạ dày", 10);
        when(topics.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(org.springframework.orm.ObjectOptimisticLockingFailureException.class,
                () -> service.update(1L, new ArticleTopicRequest("Dạ dày mới", 10, 999L)));
    }

    @Test
    void deleteBlockedWhenArticlesStillUseTopic() {
        ArticleTopic existing = topic(1L, "Dạ dày", 10);
        when(topics.findById(1L)).thenReturn(Optional.of(existing));
        when(articles.countByCategory("Dạ dày")).thenReturn(2L);

        assertThrows(IllegalStateException.class, () -> service.delete(1L));
        verify(topics, never()).delete(any());
    }

    @Test
    void deleteUnusedTopicSucceeds() {
        ArticleTopic existing = topic(1L, "Dạ dày", 10);
        when(topics.findById(1L)).thenReturn(Optional.of(existing));
        when(articles.countByCategory("Dạ dày")).thenReturn(0L);

        service.delete(1L);

        verify(topics).delete(existing);
    }

    @Test
    void deleteMissingTopicIs404() {
        when(topics.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(5L));
    }
}
