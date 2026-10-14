package vn.gastroai.be.application.knowledge;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.api.cms.knowledge.ArticleTopicRequest;
import vn.gastroai.be.api.cms.knowledge.ArticleTopicResponse;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.domain.knowledge.ArticleTopic;
import vn.gastroai.be.infrastructure.persistence.postgres.ArticleTopicRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.KnowledgeArticleRepository;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** UC0066 - CRUD chu de/danh muc bai viet cho Admin/Bac si. */
@Service
public class ArticleTopicService {

    private static final String TX = "postgresTransactionManager";
    private static final int SORT_STEP = 10;

    private final ArticleTopicRepository topicRepository;
    private final KnowledgeArticleRepository articleRepository;

    public ArticleTopicService(ArticleTopicRepository topicRepository,
                               KnowledgeArticleRepository articleRepository) {
        this.topicRepository = topicRepository;
        this.articleRepository = articleRepository;
    }

    @Transactional(value = TX, readOnly = true)
    public List<ArticleTopicResponse> list() {
        Map<String, Long> counts = new HashMap<>();
        for (Object[] row : articleRepository.countByCategoryGrouped()) {
            counts.put((String) row[0], (Long) row[1]);
        }
        return topicRepository.findAllByOrderBySortOrderAscNameAsc().stream()
                .map(t -> toResponse(t, counts.getOrDefault(t.getName(), 0L)))
                .toList();
    }

    @Transactional(TX)
    public ArticleTopicResponse create(ArticleTopicRequest request) {
        String name = cleanName(request.name());
        if (topicRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalStateException("Chu de \"" + name + "\" da ton tai");
        }
        int sortOrder = request.sortOrder() != null ? request.sortOrder() : nextSortOrder();
        ArticleTopic saved = topicRepository.save(new ArticleTopic(name, sortOrder));
        return toResponse(saved, 0L);
    }

    @Transactional(TX)
    public ArticleTopicResponse update(Long id, ArticleTopicRequest request) {
        ArticleTopic topic = load(id);
        if (request.version() != null && !request.version().equals(topic.getVersion())) {
            throw new org.springframework.orm.ObjectOptimisticLockingFailureException(ArticleTopic.class, id);
        }
        String newName = cleanName(request.name());
        if (topicRepository.existsByNameIgnoreCaseAndIdNot(newName, id)) {
            throw new IllegalStateException("Chu de \"" + newName + "\" da ton tai");
        }
        String oldName = topic.getName();
        if (!oldName.equals(newName)) {
            // Cac bai viet luu category dang chuoi ten, phai doi theo de khong bi mo coi.
            articleRepository.renameCategory(oldName, newName);
            topic.setName(newName);
        }
        if (request.sortOrder() != null) {
            topic.setSortOrder(request.sortOrder());
        }
        topic.setUpdatedAt(Instant.now());
        ArticleTopic saved = topicRepository.save(topic);
        return toResponse(saved, articleRepository.countByCategory(saved.getName()));
    }

    @Transactional(TX)
    public void delete(Long id) {
        ArticleTopic topic = load(id);
        long inUse = articleRepository.countByCategory(topic.getName());
        if (inUse > 0) {
            throw new IllegalStateException("Khong the xoa chu de \"" + topic.getName() + "\" vi con "
                    + inUse + " bai viet dang dung. Hay chuyen cac bai viet sang chu de khac truoc.");
        }
        try {
            topicRepository.delete(topic);
            topicRepository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new IllegalStateException("Khong the xoa chu de \"" + topic.getName() + "\" vi con bai viet dang dung.", e);
        }
    }

    private ArticleTopic load(Long id) {
        return topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay chu de"));
    }

    private int nextSortOrder() {
        return topicRepository.findAllByOrderBySortOrderAscNameAsc().stream()
                .mapToInt(ArticleTopic::getSortOrder)
                .max()
                .orElse(0) + SORT_STEP;
    }

    /** Gop khoang trang thua de "Da   day " va "Da day" khong thanh 2 chu de khac nhau. */
    private static String cleanName(String raw) {
        String name = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Ten chu de khong duoc de trong");
        }
        return name;
    }

    private static ArticleTopicResponse toResponse(ArticleTopic topic, long articleCount) {
        return new ArticleTopicResponse(topic.getId(), topic.getName(), topic.getSortOrder(), articleCount, topic.getVersion());
    }
}
