package vn.gastroai.be.api.cms.knowledge;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.gastroai.be.application.knowledge.FaqAdminService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cms/faqs")
public class FaqAdminController {

    private final FaqAdminService faqAdminService;

    public FaqAdminController(FaqAdminService faqAdminService) {
        this.faqAdminService = faqAdminService;
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
        return ResponseEntity.status(HttpStatus.CREATED).body(faqAdminService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CmsFaqResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CmsFaqRequest request
    ) {
        return ResponseEntity.ok(faqAdminService.update(id, request));
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<CmsFaqResponse> setPublished(
            @PathVariable Long id,
            @Valid @RequestBody CmsFaqPublishRequest request
    ) {
        return ResponseEntity.ok(faqAdminService.setPublished(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        faqAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
