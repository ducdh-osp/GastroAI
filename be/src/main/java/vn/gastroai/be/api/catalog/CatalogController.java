package vn.gastroai.be.api.catalog;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.domain.catalog.CatalogType;
import vn.gastroai.be.infrastructure.persistence.postgres.CatalogItemRepository;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalogs")
public class CatalogController {

    private final CatalogItemRepository catalogItemRepository;

    public CatalogController(CatalogItemRepository catalogItemRepository) {
        this.catalogItemRepository = catalogItemRepository;
    }

    @GetMapping("/{type}")
    public List<CatalogItemResponse> list(@PathVariable CatalogType type) {
        return catalogItemRepository.findByTypeAndActiveTrueOrderBySortOrderAscNameAsc(type)
                .stream().map(CatalogItemResponse::from).toList();
    }
}
