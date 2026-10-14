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
import vn.gastroai.be.application.knowledge.ArticleTopicService;

import java.util.List;

/** UC0066 - quan ly chu de bai viet. Quyen ADMIN/DOCTOR duoc khai bao trong SecurityConfig. */
@RestController
@RequestMapping("/api/v1/cms/article-topics")
public class ArticleTopicAdminController {

    private final ArticleTopicService topicService;

    public ArticleTopicAdminController(ArticleTopicService topicService) {
        this.topicService = topicService;
    }

    @GetMapping
    public List<ArticleTopicResponse> list() {
        return topicService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ArticleTopicResponse create(@Valid @RequestBody ArticleTopicRequest request) {
        return topicService.create(request);
    }

    @PutMapping("/{id}")
    public ArticleTopicResponse update(@PathVariable Long id, @Valid @RequestBody ArticleTopicRequest request) {
        return topicService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        topicService.delete(id);
    }
}
