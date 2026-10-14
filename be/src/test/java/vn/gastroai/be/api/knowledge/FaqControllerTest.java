package vn.gastroai.be.api.knowledge;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import vn.gastroai.be.application.knowledge.FaqPatientService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class FaqControllerTest {

    private final FaqPatientService service = mock(FaqPatientService.class);
    private final FaqController controller = new FaqController(service);

    @Test
    void listDelegatesToService() {
        when(service.list("q", "Dạ dày")).thenReturn(List.of(
                new FaqResponse(1L, "Q", "A", "Dạ dày", 10)
        ));

        ResponseEntity<List<FaqResponse>> res = controller.list("q", "Dạ dày");
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(1, res.getBody().size());
        verify(service).list("q", "Dạ dày");
    }

    @Test
    void categoriesDelegatesToService() {
        when(service.listCategories()).thenReturn(List.of("Dạ dày", "Đại tràng"));

        ResponseEntity<List<String>> res = controller.categories();
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(2, res.getBody().size());
        verify(service).listCategories();
    }
}
