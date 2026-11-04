package vn.gastroai.be.api.cms.catalog;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
import vn.gastroai.be.application.catalog.CmsFodmapAdminService;
import vn.gastroai.be.domain.audit.AuditAction;
import vn.gastroai.be.domain.catalog.FodmapLevel;

@RestController
@RequestMapping("/api/v1/cms/fodmap-foods")
public class FodmapFoodAdminController {

    private static final String TARGET = "FODMAP_FOOD";
    private static final String TARGET_UNKNOWN = "FODMAP_UNKNOWN";

    private final CmsFodmapAdminService fodmapAdminService;
    private final AuditLogService auditLogService;

    public FodmapFoodAdminController(CmsFodmapAdminService fodmapAdminService,
                                     AuditLogService auditLogService) {
        this.fodmapAdminService = fodmapAdminService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public List<FodmapFoodResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) FodmapLevel level,
            @RequestParam(required = false) Boolean active
    ) {
        return fodmapAdminService.list(q, level, active);
    }

    @GetMapping("/{id}")
    public FodmapFoodResponse getById(@PathVariable Long id) {
        return fodmapAdminService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FodmapFoodResponse create(@Valid @RequestBody FodmapFoodRequest request) {
        FodmapFoodResponse created = fodmapAdminService.create(request);
        auditLogService.record(AuditAction.CONTENT_CREATE, TARGET, created.id(), label(created));
        return created;
    }

    @PutMapping("/{id}")
    public FodmapFoodResponse update(@PathVariable Long id, @Valid @RequestBody FodmapFoodRequest request) {
        FodmapFoodResponse before = fodmapAdminService.getById(id);
        FodmapFoodResponse after = fodmapAdminService.update(id, request);
        auditLogService.record(AuditAction.CONTENT_UPDATE, TARGET, id, describeUpdate(before, after));
        return after;
    }

    @PatchMapping("/{id}/toggle-active")
    public FodmapFoodResponse toggleActive(@PathVariable Long id) {
        FodmapFoodResponse result = fodmapAdminService.toggleActive(id);
        auditLogService.record(AuditAction.CONTENT_TOGGLE_ACTIVE, TARGET, id,
                label(result) + (result.active() ? " · tắt → bật" : " · bật → tắt"));
        return result;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        FodmapFoodResponse before = fodmapAdminService.getById(id);
        fodmapAdminService.delete(id);
        auditLogService.record(AuditAction.CONTENT_DELETE, TARGET, id, label(before));
    }

    @GetMapping("/unknown-names")
    public List<FodmapUnknownNameResponse> listUnknown() {
        return fodmapAdminService.listUnknown();
    }

    @PatchMapping("/unknown-names/{id}/dismiss")
    public FodmapUnknownNameResponse dismissUnknown(@PathVariable Long id) {
        FodmapUnknownNameResponse result = fodmapAdminService.dismissUnknown(id);
        auditLogService.record(AuditAction.CONTENT_UPDATE, TARGET_UNKNOWN, id,
                "«" + result.name() + "» · bỏ qua (gặp " + result.hitCount() + " lần)");
        return result;
    }

    static String label(FodmapFoodResponse food) {
        return "«" + food.name() + "» · " + food.levelLabel();
    }

    static String describeUpdate(FodmapFoodResponse before, FodmapFoodResponse after) {
        List<String> changes = new ArrayList<>();
        if (!before.name().equals(after.name())) {
            changes.add("Tên: «" + before.name() + "» → «" + after.name() + "»");
        }
        if (before.level() != after.level()) {
            changes.add("Mức: " + before.levelLabel() + " → " + after.levelLabel());
        }
        if (!before.groups().equals(after.groups())) {
            changes.add("Nhóm: " + join(before.groups()) + " → " + join(after.groups()));
        }
        if (!before.aliases().equals(after.aliases())) {
            changes.add("Tên gọi khác: " + join(before.aliases()) + " → " + join(after.aliases()));
        }
        if (!Objects.equals(before.servingNote(), after.servingNote())) {
            changes.add("Khẩu phần: " + orEmpty(before.servingNote()) + " → " + orEmpty(after.servingNote()));
        }
        if (!Objects.equals(before.source(), after.source())) {
            changes.add("Nguồn: đã sửa");
        }
        if (!Objects.equals(before.note(), after.note())) {
            changes.add("Ghi chú: đã sửa");
        }
        return changes.isEmpty() ? label(after) : label(after) + " · " + String.join("; ", changes);
    }

    private static String join(List<String> values) {
        return values.isEmpty() ? "(trống)" : String.join(", ", values);
    }

    private static String orEmpty(String value) {
        return value == null ? "(trống)" : value;
    }
}