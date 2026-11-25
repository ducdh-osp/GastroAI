package vn.gastroai.be.api.cms.knowledge;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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
import org.springframework.web.server.ResponseStatusException;
import vn.gastroai.be.application.audit.AuditLogService;
import vn.gastroai.be.application.knowledge.KnowledgeArticleAdminService;
import vn.gastroai.be.domain.admin.Admin;
import vn.gastroai.be.domain.audit.AuditAction;
import vn.gastroai.be.domain.knowledge.ArticleStatus;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminRepository;

/** UC0065 - Quan ly bai viet kien thuc tieu hoa cho Admin / Bien tap vien. */
@RestController
@RequestMapping("/api/v1/cms/articles")
public class KnowledgeArticleAdminController {

    private static final String TARGET = "ARTICLE";

    private final KnowledgeArticleAdminService adminService;
    private final AuditLogService auditLogService;
    private final AdminRepository adminRepository;

    public KnowledgeArticleAdminController(KnowledgeArticleAdminService adminService,
                                           AuditLogService auditLogService,
                                           AdminRepository adminRepository) {
        this.adminService = adminService;
        this.auditLogService = auditLogService;
        this.adminRepository = adminRepository;
    }

    @GetMapping
    public CmsArticlePageResponse list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean published,
            @RequestParam(required = false) ArticleStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<CmsArticleSummaryResponse> result = status != null
                ? adminService.list(q, category, published, status, page, size)
                : adminService.list(q, category, published, page, size);
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
    public CmsArticleDetailResponse update(
            @PathVariable Long id,
            @Valid @RequestBody CmsArticleRequest request,
            Authentication authentication) {
        CmsArticleDetailResponse before = adminService.getById(id);
        boolean hasReviewerInput = request.medicalReviewer() != null && !request.medicalReviewer().isBlank();
        if (!canApproveContent(authentication)
                && (hasReviewerInput
                || (!before.published() && Boolean.TRUE.equals(request.published()))
                || request.status() == ArticleStatus.PUBLISHED)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản biên soạn không có quyền duyệt hoặc xuất bản bài viết");
        }

        CmsArticleRequest effective = request;
        if (hasReviewerInput) {
            String boundReviewer = resolveAuthenticatedReviewer(authentication, request.medicalReviewer());
            effective = new CmsArticleRequest(
                    request.title(),
                    request.slug(),
                    request.summary(),
                    request.category(),
                    request.content(),
                    request.sourceName(),
                    request.sourceUrl(),
                    request.sourceCheckedAt(),
                    request.published(),
                    boundReviewer,
                    request.sortOrder(),
                    request.version(),
                    request.status()
            );
        }

        CmsArticleDetailResponse after = adminService.update(id, effective);
        auditLogService.record(AuditAction.CONTENT_UPDATE, TARGET, id, describeUpdate(before, after));
        return after;
    }

    private boolean canApproveContent(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())
                        || "ROLE_CONTENT_EDITOR".equals(a.getAuthority()));
    }

    @PatchMapping("/{id}/submit-review")
    public CmsArticleDetailResponse submitForReview(@PathVariable Long id) {
        CmsArticleDetailResponse result = adminService.submitForReview(id);
        auditLogService.record(AuditAction.CONTENT_UPDATE, TARGET, id, "«" + result.title() + "» · gửi duyệt");
        return result;
    }

    @PatchMapping("/{id}/publish")
    public CmsArticleDetailResponse setPublished(
            @PathVariable Long id,
            @Valid @RequestBody CmsArticlePublishRequest request,
            Authentication authentication) {
        CmsArticlePublishRequest effective = request;
        if (request.published()) {
            String boundReviewer = resolveAuthenticatedReviewer(authentication, request.medicalReviewer());
            effective = new CmsArticlePublishRequest(true, boundReviewer, request.version());
        }
        CmsArticleDetailResponse result = adminService.setPublished(id, effective);
        String detail = "«" + result.title() + "»";
        if (result.published() && result.medicalReviewer() != null) {
            detail += " · người duyệt: " + result.medicalReviewer();
        }
        auditLogService.record(request.published() ? AuditAction.CONTENT_PUBLISH : AuditAction.CONTENT_UNPUBLISH,
                TARGET, id, detail);
        return result;
    }

    private String resolveAuthenticatedReviewer(Authentication authentication, String fallback) {
        if (authentication != null && authentication.getName() != null) {
            try {
                Long adminId = Long.valueOf(authentication.getName());
                Admin admin = adminRepository.findById(adminId).orElse(null);
                if (admin != null) {
                    String name = admin.getFullName() != null && !admin.getFullName().isBlank()
                            ? admin.getFullName().trim()
                            : admin.getEmail();
                    return name + " (" + admin.getEmail() + ")";
                }
            } catch (NumberFormatException ignored) {
                // Mock user in tests
            }
        }
        return fallback;
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