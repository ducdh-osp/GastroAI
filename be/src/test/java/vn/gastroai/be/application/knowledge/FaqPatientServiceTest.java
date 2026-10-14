package vn.gastroai.be.application.knowledge;

import org.junit.jupiter.api.Test;
import vn.gastroai.be.api.knowledge.FaqResponse;
import vn.gastroai.be.domain.knowledge.Faq;
import vn.gastroai.be.infrastructure.persistence.postgres.FaqRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FaqPatientServiceTest {

    private final FaqRepository faqRepository = mock(FaqRepository.class);
    private final FaqPatientService service = new FaqPatientService(faqRepository);

    private Faq faq(Long id, String q, String a, String category, int sortOrder) {
        Faq f = new Faq(q, a, category, sortOrder, true);
        f.setId(id);
        return f;
    }

    @Test
    void listFiltersByCategoryAndUnaccentedSearch() {
        Faq f1 = faq(1L, "Trào ngược dạ dày thực quản là gì?", "Định nghĩa trào ngược", "Trào ngược", 10);
        Faq f2 = faq(2L, "Dạ dày HP có lây không?", "Vi khuẩn HP lây qua đường ăn uống", "Dạ dày", 20);

        when(faqRepository.findByPublishedTrueOrderBySortOrderAscIdAsc()).thenReturn(List.of(f1, f2));

        // Search unaccented
        List<FaqResponse> res1 = service.list("da day hp", null);
        assertEquals(1, res1.size());
        assertEquals(2L, res1.get(0).id());

        // Filter category
        List<FaqResponse> res2 = service.list(null, "Trào ngược");
        assertEquals(1, res2.size());
        assertEquals(1L, res2.get(0).id());
    }

    @Test
    void listCategoriesReturnsDistinctPublishedCategories() {
        when(faqRepository.findDistinctPublishedCategories()).thenReturn(List.of("Dạ dày", "Trào ngược"));
        List<String> categories = service.listCategories();
        assertEquals(List.of("Dạ dày", "Trào ngược"), categories);
    }
}
