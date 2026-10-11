package vn.gastroai.be.api.catalog;

import org.junit.jupiter.api.Test;
import vn.gastroai.be.domain.catalog.CatalogItem;
import vn.gastroai.be.domain.catalog.CatalogType;
import vn.gastroai.be.infrastructure.persistence.postgres.CatalogItemRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogControllerTest {

    private final CatalogItemRepository repository = mock(CatalogItemRepository.class);
    private final CatalogController controller = new CatalogController(repository);

    @Test
    void acceptsLowercaseCategoryAndReturnsTriageMapping() {
        CatalogItem item = new CatalogItem();
        item.setId(1L);
        item.setType(CatalogType.SYMPTOM);
        item.setCode("BLOOD_IN_VOMIT");
        item.setName("Nôn ra máu");
        item.setTriageCode("BLOOD_IN_VOMIT");
        when(repository.findByTypeAndActiveTrueOrderBySortOrderAscNameAsc(CatalogType.SYMPTOM))
                .thenReturn(List.of(item));

        List<CatalogItemResponse> result = controller.list("symptom");

        assertEquals("BLOOD_IN_VOMIT", result.getFirst().triageCode());
        verify(repository).findByTypeAndActiveTrueOrderBySortOrderAscNameAsc(CatalogType.SYMPTOM);
    }

    @Test
    void rejectsUnknownCategory() {
        assertThrows(IllegalArgumentException.class, () -> controller.list("unknown"));
    }
}
