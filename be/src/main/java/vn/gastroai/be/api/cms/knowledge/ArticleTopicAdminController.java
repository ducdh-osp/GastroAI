package vn.gastroai.be.api.cms.knowledge;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.application.audit.AuditLogService;
import vn.gastroai.be.application.knowledge.ArticleTopicService;
import vn.gastroai.be.domain.audit.AuditAction;

import java.util.ArrayList;
import java.util.List;

/** UC0066 - quan ly chu de bai viet. Quyen ADMIN/DOCTOR duoc khai bao trong SecurityConfig. */
@RestController
@RequestMapping("/api/v1/cms/article-topics")
public class ArticleTopicAdminController {

    private static final String TARGET = "ARTICLE_TOPIC";

    private final ArticleTopicService topicService;
    private final AuditLogService auditLogService;

    public ArticleTopicAdminController(ArticleTopicService topicService, AuditLogService auditLogService) {
        this.topicService = topicService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public List<ArticleTopicResponse> list() {
        return topicService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ArticleTopicResponse create(@Valid @RequestBody ArticleTopicRequest request) {
        ArticleTopicResponse created = topicService.create(request);
        auditLogService.record(AuditAction.CONTENT_CREATE, TARGET, created.id(), "«" + created.name() + "»");
        return created;
    }

    @PutMapping("/{id}")
    public ArticleTopicResponse update(@PathVariable Long id, @Valid @RequestBody ArticleTopicRequest request) {
        ArticleTopicResponse before = findCurrent(id);
        ArticleTopicResponse after = topicService.update(id, request);
        auditLogService.record(AuditAction.CONTENT_UPDATE, TARGET, id,
                before == null ? "«" + after.name() + "»" : describeUpdate(before, after));
        return after;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        ArticleTopicResponse before = findCurrent(id);
        topicService.delete(id);
        auditLogService.record(AuditAction.CONTENT_DELETE, TARGET, id,
                before == null ? null : "«" + before.name() + "»");
    }

    static String describeUpdate(ArticleTopicResponse before, ArticleTopicResponse after) {
        List<String> changes = new ArrayList<>();
        if (!before.name().equals(after.name())) {
            changes.add("Tên: «" + before.name() + "» → «" + after.name() + "»");
        }
        if (before.sortOrder() != after.sortOrder()) {
            changes.add("Thứ tự: " + before.sortOrder() + " → " + after.sortOrder());
        }
        return changes.isEmpty() ? "«" + after.name() + "»" : String.join("; ", changes);
    }

    private ArticleTopicResponse findCurrent(Long id) {
        return topicService.list().stream()
                .filter(topic -> topic.id().equals(id))
                .findFirst()
                .orElse(null);
    }
}