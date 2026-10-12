package vn.gastroai.be.api.cms.knowledge;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import vn.gastroai.be.application.knowledge.KnowledgeArticleAdminService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeArticleAdminControllerTest {

    private final KnowledgeArticleAdminService service = mock(KnowledgeArticleAdminService.class);
    private final KnowledgeArticleAdminController controller = new KnowledgeArticleAdminController(service);

    private CmsArticleDetailResponse sampleDetail(Long id) {
        return new CmsArticleDetailResponse(
                id,
                "trao-nguoc",
                "Trào ngược",
                "Tóm tắt",
                "Dạ dày",
                3,
                "Nội dung",
                "Nguồn",
                "https://example.com",
                LocalDate.now(),
                true,
                "BS. A",
                Instant.now(),
                10,
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    void listDelegatesToService() {
        CmsArticleSummaryResponse item = new CmsArticleSummaryResponse(
                1L, "trao-nguoc", "Trào ngược", "Tóm tắt", "Dạ dày", 3, true, "BS. A", Instant.now(), 10, Instant.now()
        );
        when(service.list("trao", "Da day", true, 0, 20))
                .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));

        CmsArticlePageResponse res = controller.list("trao", "Da day", true, 0, 20);

        assertEquals(1, res.totalElements());
        assertEquals(1L, res.items().getFirst().id());
        verify(service).list("trao", "Da day", true, 0, 20);
    }

    @Test
    void getByIdDelegatesToService() {
        when(service.getById(5L)).thenReturn(sampleDetail(5L));

        CmsArticleDetailResponse res = controller.getById(5L);

        assertEquals(5L, res.id());
        verify(service).getById(5L);
    }

    @Test
    void createDelegatesToService() {
        CmsArticleRequest req = new CmsArticleRequest(
                "Tiêu đề", null, "Tóm tắt", "Dạ dày", 3, "Nội dung", "Nguồn", "https://example.com", null, true, null, 10
        );
        when(service.create(req)).thenReturn(sampleDetail(1L));

        CmsArticleDetailResponse res = controller.create(req);

        assertEquals(1L, res.id());
        verify(service).create(req);
    }

    @Test
    void updateDelegatesToService() {
        CmsArticleRequest req = new CmsArticleRequest(
                "Tiêu đề mới", null, "Tóm tắt mới", "Dạ dày", 3, "Nội dung", "Nguồn", "https://example.com", null, true, null, 10
        );
        when(service.update(1L, req)).thenReturn(sampleDetail(1L));

        CmsArticleDetailResponse res = controller.update(1L, req);

        assertEquals(1L, res.id());
        verify(service).update(1L, req);
    }

    @Test
    void setPublishedDelegatesToService() {
        CmsArticlePublishRequest req = new CmsArticlePublishRequest(true, "BS. B");
        when(service.setPublished(1L, req)).thenReturn(sampleDetail(1L));

        CmsArticleDetailResponse res = controller.setPublished(1L, req);

        assertEquals(1L, res.id());
        verify(service).setPublished(1L, req);
    }

    @Test
    void deleteDelegatesToService() {
        controller.delete(1L);
        verify(service).delete(1L);
    }
}
