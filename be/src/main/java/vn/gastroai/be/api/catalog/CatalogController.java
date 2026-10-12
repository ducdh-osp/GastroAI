package vn.gastroai.be.api.catalog;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.domain.catalog.CatalogType;
import vn.gastroai.be.infrastructure.persistence.postgres.CatalogItemRepository;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/catalogs")
public class CatalogController {

    private final CatalogItemRepository catalogItemRepository;

    public CatalogController(CatalogItemRepository catalogItemRepository) {
        this.catalogItemRepository = catalogItemRepository;
    }

    @GetMapping("/{type}")
    public List<CatalogItemResponse> list(@PathVariable String type) {
        CatalogType normalizedType;
        try {
            normalizedType = CatalogType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("type phai la SYMPTOM, CONDITION hoac MEDICATION");
        }
        return catalogItemRepository.findByTypeAndActiveTrueOrderBySortOrderAscNameAsc(normalizedType)
                .stream().map(CatalogItemResponse::from).toList();
    }
}
