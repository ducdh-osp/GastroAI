package vn.gastroai.be.api.cms.triage;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.application.triage.TriageKeywordGroupResponse;
import vn.gastroai.be.application.triage.TriageKeywordRequest;
import vn.gastroai.be.application.triage.TriageKeywordResponse;
import vn.gastroai.be.application.triage.TriageKeywordService;

import java.util.List;

/** UC0054 - Admin/Bác sĩ CRUD từ khóa dấu hiệu cấp cứu dùng cho tầng 1 Triage. */
@RestController
@RequestMapping("/api/v1/cms/triage-keywords")
public class TriageKeywordAdminController {

    private final TriageKeywordService triageKeywordService;

    public TriageKeywordAdminController(TriageKeywordService triageKeywordService) {
        this.triageKeywordService = triageKeywordService;
    }

    @GetMapping
    public List<TriageKeywordResponse> list() {
        return triageKeywordService.list();
    }

    @GetMapping("/groups")
    public List<TriageKeywordGroupResponse> listGroups() {
        return triageKeywordService.listGroups();
    }

    @PostMapping
    public ResponseEntity<TriageKeywordResponse> create(@RequestBody TriageKeywordRequest request) {
        return ResponseEntity.ok(triageKeywordService.create(request));
    }

    @PutMapping("/{id}")
    public TriageKeywordResponse update(@PathVariable Long id, @RequestBody TriageKeywordRequest request) {
        return triageKeywordService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        triageKeywordService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
