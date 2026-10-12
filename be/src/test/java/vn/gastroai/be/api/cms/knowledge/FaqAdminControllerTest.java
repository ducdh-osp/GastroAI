package vn.gastroai.be.api.cms.knowledge;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import vn.gastroai.be.application.knowledge.FaqAdminService;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class FaqAdminControllerTest {

    private final FaqAdminService service = mock(FaqAdminService.class);
    private final FaqAdminController controller = new FaqAdminController(service);

    @Test
    void listDelegatesToService() {
        CmsFaqPageResponse pageResponse = new CmsFaqPageResponse(
                List.of(new CmsFaqResponse(1L, "Q", "A", "Cat", 10, true, Instant.now(), Instant.now())),
                1,
                0,
                20
        );
        when(service.list("q", "Cat", true, 0, 20)).thenReturn(pageResponse);

        ResponseEntity<CmsFaqPageResponse> res = controller.list("q", "Cat", true, 0, 20);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(1, res.getBody().total());
        verify(service).list("q", "Cat", true, 0, 20);
    }

    @Test
    void createDelegatesToService() {
        CmsFaqRequest req = new CmsFaqRequest("Q", "A", "Cat", 10, true);
        CmsFaqResponse response = new CmsFaqResponse(1L, "Q", "A", "Cat", 10, true, Instant.now(), Instant.now());
        when(service.create(req)).thenReturn(response);

        ResponseEntity<CmsFaqResponse> res = controller.create(req);
        assertEquals(HttpStatus.CREATED, res.getStatusCode());
        assertEquals(1L, res.getBody().id());
        verify(service).create(req);
    }

    @Test
    void setPublishedDelegatesToService() {
        CmsFaqResponse response = new CmsFaqResponse(1L, "Q", "A", "Cat", 10, false, Instant.now(), Instant.now());
        when(service.setPublished(1L, false)).thenReturn(response);

        ResponseEntity<CmsFaqResponse> res = controller.setPublished(1L, new CmsFaqPublishRequest(false));
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(false, res.getBody().published());
        verify(service).setPublished(1L, false);
    }

    @Test
    void deleteDelegatesToService() {
        ResponseEntity<Void> res = controller.delete(1L);
        assertEquals(HttpStatus.NO_CONTENT, res.getStatusCode());
        verify(service).delete(1L);
    }
}
