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
import vn.gastroai.be.application.audit.AuditLogService;
import vn.gastroai.be.application.catalog.CmsCatalogAdminService;
import vn.gastroai.be.domain.audit.AuditAction;
import vn.gastroai.be.domain.catalog.CatalogType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * UC0060 - API CRUD Danh mục chuẩn hóa (triệu chứng, bệnh lý, thuốc) cho CMS.
 */
@RestController
@RequestMapping("/api/v1/cms/catalogs")
public class CmsCatalogAdminController {

    private static final String TARGET = "CATALOG";

    private final CmsCatalogAdminService catalogAdminService;
    private final AuditLogService auditLogService;

    public CmsCatalogAdminController(CmsCatalogAdminService catalogAdminService,
                                     AuditLogService auditLogService) {
        this.catalogAdminService = catalogAdminService;
        this.auditLogService = auditLogService;
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
        CmsCatalogItemResponse created = catalogAdminService.create(request);
        auditLogService.record(AuditAction.CONTENT_CREATE, TARGET, created.id(), label(created));
        return created;
    }

    @PutMapping("/{id}")
    public CmsCatalogItemResponse update(@PathVariable Long id, @Valid @RequestBody CmsCatalogItemRequest request) {
        CmsCatalogItemResponse before = catalogAdminService.getById(id);
        CmsCatalogItemResponse after = catalogAdminService.update(id, request);
        auditLogService.record(AuditAction.CONTENT_UPDATE, TARGET, id, describeUpdate(before, after));
        return after;
    }

    @PatchMapping("/{id}/toggle-active")
    public CmsCatalogItemResponse toggleActive(@PathVariable Long id) {
        CmsCatalogItemResponse result = catalogAdminService.toggleActive(id);
        auditLogService.record(AuditAction.CONTENT_TOGGLE_ACTIVE, TARGET, id,
                label(result) + (result.active() ? " · tắt → bật" : " · bật → tắt"));
        return result;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        CmsCatalogItemResponse before = catalogAdminService.getById(id);
        catalogAdminService.delete(id);
        auditLogService.record(AuditAction.CONTENT_DELETE, TARGET, id, label(before));
    }

    static String label(CmsCatalogItemResponse item) {
        return "[" + item.type() + "] " + item.code() + " – " + item.name();
    }

    static String describeUpdate(CmsCatalogItemResponse before, CmsCatalogItemResponse after) {
        List<String> changes = new ArrayList<>();
        if (!before.code().equals(after.code())) {
            changes.add("Mã: " + before.code() + " → " + after.code());
        }
        if (!before.name().equals(after.name())) {
            changes.add("Tên: «" + before.name() + "» → «" + after.name() + "»");
        }
        if (!Objects.equals(before.triageCode(), after.triageCode())) {
            changes.add("Mã Triage: " + before.triageCode() + " → " + after.triageCode());
        }
        if (!Objects.equals(before.sortOrder(), after.sortOrder())) {
            changes.add("Thứ tự: " + before.sortOrder() + " → " + after.sortOrder());
        }
        return changes.isEmpty() ? label(after) : label(after) + " · " + String.join("; ", changes);
    }
}