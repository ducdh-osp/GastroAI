package vn.gastroai.be.application.knowledge;

import org.junit.jupiter.api.Test;
import vn.gastroai.be.api.cms.knowledge.CmsFaqPageResponse;
import vn.gastroai.be.api.cms.knowledge.CmsFaqRequest;
import vn.gastroai.be.api.cms.knowledge.CmsFaqResponse;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.domain.knowledge.Faq;
import vn.gastroai.be.infrastructure.persistence.postgres.FaqRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FaqAdminServiceTest {

    private final FaqRepository faqRepository = mock(FaqRepository.class);
    private final FaqAdminService service = new FaqAdminService(faqRepository);

    private Faq faq(Long id, String q, String a, String category, int sortOrder, boolean published) {
        Faq f = new Faq(q, a, category, sortOrder, published);
        f.setId(id);
        return f;
    }

    @Test
    void listFiltersByPublishedAndNormalizedSearch() {
        Faq f1 = faq(1L, "Trào ngược dạ dày có nguy hiểm?", "Câu trả lời trào ngược", "Trào ngược", 10, true);
        Faq f2 = faq(2L, "Đau dạ dày nên ăn gì?", "Nên ăn cháo nhẹ", "Dạ dày", 20, false);
        Faq f3 = faq(3L, "Đại tràng co thắt là gì?", "Hội chứng ruột kích thích", "Đại tràng", 30, true);

        when(faqRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(f1, f2, f3));

        // Filter published = true only
        CmsFaqPageResponse res1 = service.list(null, null, true, 0, 10);
        assertEquals(2, res1.total());
        assertEquals(List.of(1L, 3L), res1.items().stream().map(CmsFaqResponse::id).toList());

        // Search unaccented "trao nguoc"
        CmsFaqPageResponse res2 = service.list("trao nguoc", null, null, 0, 10);
        assertEquals(1, res2.total());
        assertEquals(1L, res2.items().get(0).id());
    }

    @Test
    void createAutoAssignsSortOrder() {
        Faq existing = faq(1L, "Q1", "A1", "Cat1", 50, true);
        when(faqRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(existing));
        when(faqRepository.save(any(Faq.class))).thenAnswer(inv -> {
            Faq saved = inv.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        CmsFaqRequest req = new CmsFaqRequest("  Câu hỏi mới  ", "Trả lời mới", "  Dạ dày  ", null, null);
        CmsFaqResponse res = service.create(req);

        assertEquals("Câu hỏi mới", res.question());
        assertEquals("Dạ dày", res.category());
        assertEquals(60, res.sortOrder());
        assertTrue(res.published());
    }

    @Test
    void updateModifiesFields() {
        Faq existing = faq(5L, "Q cũ", "A cũ", "Cat cũ", 10, true);
        when(faqRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(faqRepository.save(existing)).thenReturn(existing);

        CmsFaqRequest req = new CmsFaqRequest("Q mới", "A mới", "Cat mới", 15, false);
        CmsFaqResponse res = service.update(5L, req);

        assertEquals("Q mới", res.question());
        assertEquals("A mới", res.answer());
        assertEquals("Cat mới", res.category());
        assertEquals(15, res.sortOrder());
        assertFalse(res.published());
    }

    @Test
    void setPublishedTogglesStatus() {
        Faq existing = faq(5L, "Q", "A", "Cat", 10, true);
        when(faqRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(faqRepository.save(existing)).thenReturn(existing);

        CmsFaqResponse res = service.setPublished(5L, false);
        assertFalse(res.published());
    }

    @Test
    void deleteThrowsIfNotFound() {
        when(faqRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.delete(99L));
    }

    @Test
    void getCategoriesReturnsAllDistinctCategories() {
        when(faqRepository.findDistinctAllCategories()).thenReturn(List.of("Dạ dày", "Đại tràng"));
        List<String> categories = service.getCategories();
        assertEquals(2, categories.size());
        assertEquals("Dạ dày", categories.get(0));
    }
}
