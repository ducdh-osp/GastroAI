package vn.gastroai.be.api.cms.knowledge;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
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
import vn.gastroai.be.application.knowledge.KnowledgeArticleAdminService;
import vn.gastroai.be.domain.audit.AuditAction;

/** UC0065 - Quan ly bai viet kien thuc tieu hoa cho Admin / Bac si. */
@RestController
@RequestMapping("/api/v1/cms/articles")
public class KnowledgeArticleAdminController {

    private static final String TARGET = "ARTICLE";

    private final KnowledgeArticleAdminService adminService;
    private final AuditLogService auditLogService;

    public KnowledgeArticleAdminController(KnowledgeArticleAdminService adminService,
                                           AuditLogService auditLogService) {
        this.adminService = adminService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public CmsArticlePageResponse list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean published,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<CmsArticleSummaryResponse> result = adminService.list(q, category, published, page, size);
        return CmsArticlePageResponse.from(result);
    }

    @GetMapping("/{id}")
    public CmsArticleDetailResponse getById(@PathVariable Long id) {
        return adminService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CmsArticleDetailResponse create(@Valid @RequestBody CmsArticleRequest request) {
        CmsArticleDetailResponse created = adminService.create(request);
        auditLogService.record(AuditAction.CONTENT_CREATE, TARGET, created.id(),
                "«" + created.title() + "»" + (created.published() ? " · đăng ngay" : " · bản nháp"));
        return created;
    }

    @PutMapping("/{id}")
    public CmsArticleDetailResponse update(@PathVariable Long id, @Valid @RequestBody CmsArticleRequest request) {
        CmsArticleDetailResponse before = adminService.getById(id);
        CmsArticleDetailResponse after = adminService.update(id, request);
        auditLogService.record(AuditAction.CONTENT_UPDATE, TARGET, id, describeUpdate(before, after));
        return after;
    }

    @PatchMapping("/{id}/publish")
    public CmsArticleDetailResponse setPublished(@PathVariable Long id, @Valid @RequestBody CmsArticlePublishRequest request) {
        CmsArticleDetailResponse result = adminService.setPublished(id, request);
        String detail = "«" + result.title() + "»";
        if (result.published() && result.medicalReviewer() != null) {
            detail += " · người duyệt: " + result.medicalReviewer();
        }
        auditLogService.record(request.published() ? AuditAction.CONTENT_PUBLISH : AuditAction.CONTENT_UNPUBLISH,
                TARGET, id, detail);
        return result;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        CmsArticleDetailResponse before = adminService.getById(id);
        adminService.delete(id);
        auditLogService.record(AuditAction.CONTENT_DELETE, TARGET, id, "«" + before.title() + "»");
    }

    static String describeUpdate(CmsArticleDetailResponse before, CmsArticleDetailResponse after) {
        String detail = before.title().equals(after.title())
                ? "«" + after.title() + "»"
                : "Tiêu đề: «" + before.title() + "» → «" + after.title() + "»";
        if (before.published() && !after.published()) {
            detail += " · tự gỡ khỏi bệnh nhân (sửa nội dung chưa có người duyệt)";
        }
        return detail;
    }
}