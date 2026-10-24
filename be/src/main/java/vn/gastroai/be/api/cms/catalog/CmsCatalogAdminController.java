package vn.gastroai.be.api.cms.catalog;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.application.catalog.CmsCatalogAdminService;
import vn.gastroai.be.domain.catalog.CatalogType;

import java.util.List;

/**
 * UC0060 - API CRUD Danh mục chuẩn hóa (triệu chứng, bệnh lý, thuốc) cho CMS.
 */
@RestController
@RequestMapping("/api/v1/cms/catalogs")
public class CmsCatalogAdminController {

    private final CmsCatalogAdminService catalogAdminService;

    public CmsCatalogAdminController(CmsCatalogAdminService catalogAdminService) {
        this.catalogAdminService = catalogAdminService;
    }

    @GetMapping
    public List<CmsCatalogItemResponse> list(
            @RequestParam(required = false) CatalogType type,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean active
    ) {
        return catalogAdminService.list(type, q, active);
    }

    @GetMapping("/{id}")
    public CmsCatalogItemResponse getById(@PathVariable Long id) {
        return catalogAdminService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CmsCatalogItemResponse create(@Valid @RequestBody CmsCatalogItemRequest request) {
        return catalogAdminService.create(request);
    }

    @PutMapping("/{id}")
    public CmsCatalogItemResponse update(@PathVariable Long id, @Valid @RequestBody CmsCatalogItemRequest request) {
        return catalogAdminService.update(id, request);
    }

    @PatchMapping("/{id}/toggle-active")
    public CmsCatalogItemResponse toggleActive(@PathVariable Long id) {
        return catalogAdminService.toggleActive(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        catalogAdminService.delete(id);
    }
}
