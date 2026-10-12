package vn.gastroai.be.api.cms.knowledge;

import org.junit.jupiter.api.Test;
import vn.gastroai.be.application.knowledge.ArticleTopicService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleTopicAdminControllerTest {

    private final ArticleTopicService service = mock(ArticleTopicService.class);
    private final ArticleTopicAdminController controller = new ArticleTopicAdminController(service);

    @Test
    void listDelegatesToService() {
        when(service.list()).thenReturn(List.of(
                new ArticleTopicResponse(1L, "Dạ dày", 10, 5L)
        ));

        List<ArticleTopicResponse> result = controller.list();

        assertEquals(1, result.size());
        assertEquals("Dạ dày", result.getFirst().name());
        verify(service).list();
    }

    @Test
    void createDelegatesToService() {
        ArticleTopicRequest request = new ArticleTopicRequest("Đại tràng", 20);
        when(service.create(request)).thenReturn(new ArticleTopicResponse(2L, "Đại tràng", 20, 0L));

        ArticleTopicResponse result = controller.create(request);

        assertEquals(2L, result.id());
        assertEquals("Đại tràng", result.name());
        verify(service).create(request);
    }

    @Test
    void updateDelegatesToService() {
        ArticleTopicRequest request = new ArticleTopicRequest("Đại tràng & ruột", 25);
        when(service.update(2L, request)).thenReturn(new ArticleTopicResponse(2L, "Đại tràng & ruột", 25, 3L));

        ArticleTopicResponse result = controller.update(2L, request);

        assertEquals("Đại tràng & ruột", result.name());
        verify(service).update(2L, request);
    }

    @Test
    void deleteDelegatesToService() {
        controller.delete(3L);
        verify(service).delete(3L);
    }
}
