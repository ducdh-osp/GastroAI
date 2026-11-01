package vn.gastroai.be.api.cms.knowledge;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.gastroai.be.application.audit.AuditLogService;
import vn.gastroai.be.application.knowledge.FaqAdminService;
import vn.gastroai.be.domain.audit.AuditAction;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cms/faqs")
public class FaqAdminController {

    private static final String TARGET = "FAQ";
    private static final int MAX_QUESTION_IN_LOG = 120;

    private final FaqAdminService faqAdminService;
    private final AuditLogService auditLogService;

    public FaqAdminController(FaqAdminService faqAdminService, AuditLogService auditLogService) {
        this.faqAdminService = faqAdminService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<CmsFaqPageResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean published,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(faqAdminService.list(q, category, published, page, size));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(faqAdminService.getCategories());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CmsFaqResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(faqAdminService.get(id));
    }

    @PostMapping
    public ResponseEntity<CmsFaqResponse> create(@Valid @RequestBody CmsFaqRequest request) {
        CmsFaqResponse created = faqAdminService.create(request);
        auditLogService.record(AuditAction.CONTENT_CREATE, TARGET, created.id(),
                quote(created.question()) + (created.published() ? " · đăng ngay" : " · bản nháp"));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CmsFaqResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CmsFaqRequest request
    ) {
        CmsFaqResponse before = faqAdminService.get(id);
        CmsFaqResponse after = faqAdminService.update(id, request);
        auditLogService.record(AuditAction.CONTENT_UPDATE, TARGET, id, describeUpdate(before, after));
        return ResponseEntity.ok(after);
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<CmsFaqResponse> setPublished(
            @PathVariable Long id,
            @Valid @RequestBody CmsFaqPublishRequest request
    ) {
        CmsFaqResponse result = faqAdminService.setPublished(id, request);
        String detail = quote(result.question());
        if (result.published() && result.medicalReviewer() != null) {
            detail += " · người duyệt: " + result.medicalReviewer();
        }
        auditLogService.record(Boolean.TRUE.equals(request.published())
                ? AuditAction.CONTENT_PUBLISH : AuditAction.CONTENT_UNPUBLISH, TARGET, id, detail);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        CmsFaqResponse before = faqAdminService.get(id);
        faqAdminService.delete(id);
        auditLogService.record(AuditAction.CONTENT_DELETE, TARGET, id, quote(before.question()));
        return ResponseEntity.noContent().build();
    }

    static String describeUpdate(CmsFaqResponse before, CmsFaqResponse after) {
        String detail = before.question().equals(after.question())
                ? quote(after.question())
                : "Câu hỏi: " + quote(before.question()) + " → " + quote(after.question());
        if (before.published() && !after.published()) {
            detail += " · tự gỡ khỏi bệnh nhân (sửa nội dung chưa có người duyệt)";
        }
        return detail;
    }

    static String quote(String question) {
        String shortQuestion = question.length() <= MAX_QUESTION_IN_LOG
                ? question
                : question.substring(0, MAX_QUESTION_IN_LOG) + "…";
        return "«" + shortQuestion + "»";
    }
}