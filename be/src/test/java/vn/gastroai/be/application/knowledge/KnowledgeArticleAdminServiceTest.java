package vn.gastroai.be.application.knowledge;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import vn.gastroai.be.api.cms.knowledge.CmsArticleDetailResponse;
import vn.gastroai.be.api.cms.knowledge.CmsArticlePublishRequest;
import vn.gastroai.be.api.cms.knowledge.CmsArticleRequest;
import vn.gastroai.be.api.cms.knowledge.CmsArticleSummaryResponse;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.domain.knowledge.ArticleTopic;
import vn.gastroai.be.domain.knowledge.KnowledgeArticle;
import vn.gastroai.be.infrastructure.persistence.postgres.ArticleTopicRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.KnowledgeArticleRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KnowledgeArticleAdminServiceTest {

    private final KnowledgeArticleRepository articleRepository = mock(KnowledgeArticleRepository.class);
    private final ArticleTopicRepository topicRepository = mock(ArticleTopicRepository.class);
    private final KnowledgeArticleAdminService service = new KnowledgeArticleAdminService(articleRepository, topicRepository);

    @BeforeEach
    void setupTopicMock() {
        when(topicRepository.findByNameIgnoreCase(any())).thenAnswer(inv -> {
            String name = inv.getArgument(0);
            return Optional.of(new ArticleTopic(name, 10));
        });
    }

    private KnowledgeArticle sampleArticle(Long id, String slug, String title, String category, boolean published) {
        KnowledgeArticle a = new KnowledgeArticle();
        a.setId(id);
        a.setSlug(slug);
        a.setTitle(title);
        a.setSummary("Tóm tắt bài viết");
        a.setCategory(category);
        a.setContent("Nội dung bài viết về tiêu hoá rất chi tiết.");
        a.setSourceName("NIDDK");
        a.setSourceUrl("https://niddk.nih.gov");
        a.setSourceCheckedAt(LocalDate.of(2026, 10, 8));
        a.setPublished(published);
        a.setMedicalReviewer(published ? "BS. Nguyễn Văn A" : null);
        a.setMedicalReviewedAt(published ? Instant.now() : null);
        a.setSortOrder(10);
        a.setCreatedAt(Instant.now());
        a.setUpdatedAt(Instant.now());
        a.setVersion(0L);
        return a;
    }

    @Test
    void listWithFilteringAndPaging() {
        KnowledgeArticle a1 = sampleArticle(1L, "trao-nguoc", "Trào ngược dạ dày", "Dạ dày", true);
        KnowledgeArticle a2 = sampleArticle(2L, "hoi-chung-ruot-kich-thich", "Hội chứng ruột kích thích", "Đại tràng", false);
        when(articleRepository.findAllByOrderBySortOrderAscTitleAsc()).thenReturn(List.of(a1, a2));

        // Lọc theo published = false
        Page<CmsArticleSummaryResponse> draftPage = service.list(null, null, false, 0, 10);
        assertEquals(1, draftPage.getTotalElements());
        assertEquals(2L, draftPage.getContent().getFirst().id());

        // Lọc theo query "trao nguoc" (không dấu)
        Page<CmsArticleSummaryResponse> searchPage = service.list("trao nguoc", null, null, 0, 10);
        assertEquals(1, searchPage.getTotalElements());
        assertEquals(1L, searchPage.getContent().getFirst().id());

        // Lọc theo category "Dạ dày"
        Page<CmsArticleSummaryResponse> catPage = service.list(null, "da day", null, 0, 10);
        assertEquals(1, catPage.getTotalElements());
        assertEquals("trao-nguoc", catPage.getContent().getFirst().slug());
    }

    @Test
    void createAutoGeneratesSlugAndEnforcesMedicalReviewerForPublished() {
        when(articleRepository.existsBySlug("viem-loet-da-day")).thenReturn(false);
        when(articleRepository.findAllByOrderBySortOrderAscTitleAsc()).thenReturn(List.of());
        when(articleRepository.save(any(KnowledgeArticle.class))).thenAnswer(inv -> {
            KnowledgeArticle a = inv.getArgument(0);
            a.setId(10L);
            return a;
        });

        CmsArticleRequest req = new CmsArticleRequest(
                "Viêm loét dạ dày",
                null, // slug tự sinh
                "Tóm tắt viêm loét",
                "Dạ dày",
                "Đây là bài viết dài hơn một chút để kiểm tra tính toán thời gian đọc của hệ thống.",
                "Bệnh viện Bạch Mai",
                "https://bachmai.gov.vn",
                null,
                true,
                "BS. Nguyễn Văn A",
                null,
                null
        );

        CmsArticleDetailResponse res = service.create(req);

        assertEquals("viem-loet-da-day", res.slug());
        assertEquals("Viêm loét dạ dày", res.title());
        assertTrue(res.published());
        assertEquals("BS. Nguyễn Văn A", res.medicalReviewer());
        assertNotNull(res.medicalReviewedAt());
    }

    @Test
    void createRejectsPublishedWithoutReviewer() {
        CmsArticleRequest req = new CmsArticleRequest(
                "Viêm loét dạ dày",
                null,
                "Tóm tắt",
                "Dạ dày",
                "Nội dung bài viết",
                "Nguồn",
                "https://example.com",
                null,
                true,
                null, // Không có reviewer
                null,
                null
        );

        assertThrows(IllegalArgumentException.class, () -> service.create(req));
    }

    @Test
    void createRejectsNonExistentCategory() {
        when(topicRepository.findByNameIgnoreCase("Chủ đề lạ")).thenReturn(Optional.empty());

        CmsArticleRequest req = new CmsArticleRequest(
                "Tiêu đề",
                null,
                "Tóm tắt",
                "Chủ đề lạ",
                "Nội dung",
                "Nguồn",
                "https://example.com",
                null,
                false,
                null,
                null,
                null
        );

        assertThrows(IllegalArgumentException.class, () -> service.create(req));
    }

    @Test
    void updateUnpublishesWhenContentChangedWithoutReviewer() {
        KnowledgeArticle existing = sampleArticle(1L, "slug-cu", "Tiêu đề cũ", "Dạ dày", true);
        when(articleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(articleRepository.save(existing)).thenReturn(existing);

        CmsArticleRequest req = new CmsArticleRequest(
                "Tiêu đề mới hoàn toàn",
                null,
                "Tóm tắt mới",
                "Dạ dày",
                "Nội dung mới đã sửa chữa",
                "Nguồn",
                "https://example.com",
                null,
                true, // Muốn giữ published nhưng không đưa reviewer mới
                null,
                null,
                0L
        );

        CmsArticleDetailResponse res = service.update(1L, req);

        // Hệ thống tự động chuyển về bản nháp và xóa medicalReviewedAt để bác sĩ duyệt lại
        assertFalse(res.published());
        assertNull(res.medicalReviewedAt());
    }

    @Test
    void updateChecksOptimisticLockVersion() {
        KnowledgeArticle existing = sampleArticle(1L, "slug-cu", "Tiêu đề cũ", "Dạ dày", true);
        existing.setVersion(5L);
        when(articleRepository.findById(1L)).thenReturn(Optional.of(existing));

        CmsArticleRequest req = new CmsArticleRequest(
                "Tiêu đề",
                null,
                "Tóm tắt",
                "Dạ dày",
                "Nội dung",
                "Nguồn",
                "https://example.com",
                null,
                true,
                "BS. A",
                null,
                4L // Version cũ -> Conflict
        );

        assertThrows(ObjectOptimisticLockingFailureException.class, () -> service.update(1L, req));
    }

    @Test
    void setPublishedRequiresMedicalReviewer() {
        KnowledgeArticle existing = sampleArticle(1L, "slug-1", "Tiêu đề", "Dạ dày", false);
        existing.setMedicalReviewer(null);
        when(articleRepository.findById(1L)).thenReturn(Optional.of(existing));

        CmsArticlePublishRequest req = new CmsArticlePublishRequest(true, null, 0L);
        assertThrows(IllegalArgumentException.class, () -> service.setPublished(1L, req));
    }

    @Test
    void setPublishedTogglesStatusAndUpdatesReviewer() {
        KnowledgeArticle existing = sampleArticle(1L, "slug-1", "Tiêu đề", "Dạ dày", false);
        when(articleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(articleRepository.save(existing)).thenReturn(existing);

        CmsArticlePublishRequest req = new CmsArticlePublishRequest(true, "BS. Trần Văn B", 0L);
        CmsArticleDetailResponse res = service.setPublished(1L, req);

        assertTrue(res.published());
        assertEquals("BS. Trần Văn B", res.medicalReviewer());
        assertNotNull(res.medicalReviewedAt());
    }

    @Test
    void deleteRemovesArticle() {
        KnowledgeArticle existing = sampleArticle(1L, "slug-1", "Tiêu đề", "Dạ dày", false);
        when(articleRepository.findById(1L)).thenReturn(Optional.of(existing));

        service.delete(1L);

        verify(articleRepository).delete(existing);
    }

    @Test
    void getByIdNotFoundThrows404() {
        when(articleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getById(99L));
    }
}
