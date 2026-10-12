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
import vn.gastroai.be.application.knowledge.KnowledgeArticleAdminService;

/** UC0065 - Quan ly bai viet kien thuc tieu hoa cho Admin / Bac si. */
@RestController
@RequestMapping("/api/v1/cms/articles")
public class KnowledgeArticleAdminController {

    private final KnowledgeArticleAdminService adminService;

    public KnowledgeArticleAdminController(KnowledgeArticleAdminService adminService) {
        this.adminService = adminService;
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
        return adminService.create(request);
    }

    @PutMapping("/{id}")
    public CmsArticleDetailResponse update(@PathVariable Long id, @Valid @RequestBody CmsArticleRequest request) {
        return adminService.update(id, request);
    }

    @PatchMapping("/{id}/publish")
    public CmsArticleDetailResponse setPublished(@PathVariable Long id, @Valid @RequestBody CmsArticlePublishRequest request) {
        return adminService.setPublished(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        adminService.delete(id);
    }
}
