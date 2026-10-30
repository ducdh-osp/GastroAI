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
import vn.gastroai.be.application.audit.AuditLogService;
import vn.gastroai.be.application.triage.TriageKeywordGroupResponse;
import vn.gastroai.be.application.triage.TriageKeywordRequest;
import vn.gastroai.be.application.triage.TriageKeywordResponse;
import vn.gastroai.be.application.triage.TriageKeywordService;
import vn.gastroai.be.domain.audit.AuditAction;

import java.util.List;

/** UC0054 - Admin/Bác sĩ CRUD từ khóa dấu hiệu cấp cứu dùng cho tầng 1 Triage. */
@RestController
@RequestMapping("/api/v1/cms/triage-keywords")
public class TriageKeywordAdminController {

    private static final String TARGET = "TRIAGE_KEYWORD";

    private final TriageKeywordService triageKeywordService;
    private final AuditLogService auditLogService;

    public TriageKeywordAdminController(TriageKeywordService triageKeywordService,
                                        AuditLogService auditLogService) {
        this.triageKeywordService = triageKeywordService;
        this.auditLogService = auditLogService;
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
        TriageKeywordResponse created = triageKeywordService.create(request);
        auditLogService.record(AuditAction.TRIAGE_KEYWORD_CREATE, TARGET, created.id(), describe(created));
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public TriageKeywordResponse update(@PathVariable Long id, @RequestBody TriageKeywordRequest request) {
        TriageKeywordResponse before = findCurrent(id);
        TriageKeywordResponse updated = triageKeywordService.update(id, request);
        String detail = before == null
                ? describe(updated)
                : describe(before) + " → " + describe(updated);
        auditLogService.record(AuditAction.TRIAGE_KEYWORD_UPDATE, TARGET, id, detail);
        return updated;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        TriageKeywordResponse before = findCurrent(id);
        triageKeywordService.delete(id);
        auditLogService.record(AuditAction.TRIAGE_KEYWORD_DELETE, TARGET, id,
                before == null ? null : describe(before));
        return ResponseEntity.noContent().build();
    }

    private TriageKeywordResponse findCurrent(Long id) {
        return triageKeywordService.list().stream()
                .filter(keyword -> keyword.id().equals(id))
                .findFirst()
                .orElse(null);
    }

    private static String describe(TriageKeywordResponse keyword) {
        String group = keyword.groupLabel() != null ? keyword.groupLabel() : keyword.groupCode();
        return "[" + group + "] " + keyword.keyword();
    }
}