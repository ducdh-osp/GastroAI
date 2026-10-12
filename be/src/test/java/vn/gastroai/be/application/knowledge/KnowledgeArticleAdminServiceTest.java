package vn.gastroai.be.application.knowledge;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import vn.gastroai.be.api.cms.knowledge.CmsArticleDetailResponse;
import vn.gastroai.be.api.cms.knowledge.CmsArticlePublishRequest;
import vn.gastroai.be.api.cms.knowledge.CmsArticleRequest;
import vn.gastroai.be.api.cms.knowledge.CmsArticleSummaryResponse;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.domain.knowledge.KnowledgeArticle;
import vn.gastroai.be.infrastructure.persistence.postgres.KnowledgeArticleRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeArticleAdminServiceTest {

    private final KnowledgeArticleRepository articleRepository = mock(KnowledgeArticleRepository.class);
    private final KnowledgeArticleAdminService service = new KnowledgeArticleAdminService(articleRepository);

    private KnowledgeArticle sampleArticle(Long id, String slug, String title, String category, boolean published) {
        KnowledgeArticle a = new KnowledgeArticle();
        a.setId(id);
        a.setSlug(slug);
        a.setTitle(title);
        a.setSummary("Tóm tắt bài viết");
        a.setCategory(category);
        a.setContent("Nội dung bài viết về tiêu hoá rất chi tiết.");
        a.setReadingMinutes(3);
        a.setSourceName("NIDDK");
        a.setSourceUrl("https://niddk.nih.gov");
        a.setSourceCheckedAt(LocalDate.of(2026, 10, 8));
        a.setPublished(published);
        a.setSortOrder(10);
        a.setCreatedAt(Instant.now());
        a.setUpdatedAt(Instant.now());
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
    void createAutoGeneratesSlugAndCalculatesReadingMinutes() {
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
                null, // readingMinutes tự tính
                "Đây là bài viết dài hơn một chút để kiểm tra tính toán thời gian đọc của hệ thống.",
                "Bệnh viện Bạch Mai",
                "https://bachmai.gov.vn",
                null,
                true,
                "BS. Nguyễn Văn A",
                null
        );

        CmsArticleDetailResponse res = service.create(req);

        assertEquals("viem-loet-da-day", res.slug());
        assertEquals("Viêm loét dạ dày", res.title());
        assertEquals(1, res.readingMinutes()); // ~17 từ -> 1 phút
        assertTrue(res.published());
        assertEquals("BS. Nguyễn Văn A", res.medicalReviewer());
        assertNotNull(res.medicalReviewedAt());
    }

    @Test
    void createRejectsDuplicateManualSlug() {
        when(articleRepository.existsBySlug("custom-slug")).thenReturn(true);

        CmsArticleRequest req = new CmsArticleRequest(
                "Tiêu đề",
                "custom-slug",
                "Tóm tắt",
                "Dạ dày",
                2,
                "Nội dung",
                "Nguồn",
                "https://example.com",
                null,
                false,
                null,
                null
        );

        assertThrows(IllegalStateException.class, () -> service.create(req));
    }

    @Test
    void updateValidatesDuplicateSlugForOtherArticles() {
        KnowledgeArticle existing = sampleArticle(1L, "slug-cu", "Tiêu đề cũ", "Dạ dày", true);
        when(articleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(articleRepository.existsBySlugAndIdNot("slug-trung", 1L)).thenReturn(true);

        CmsArticleRequest req = new CmsArticleRequest(
                "Tiêu đề mới",
                "slug-trung",
                "Tóm tắt mới",
                "Dạ dày",
                5,
                "Nội dung mới",
                "Nguồn",
                "https://example.com",
                null,
                true,
                null,
                null
        );

        assertThrows(IllegalStateException.class, () -> service.update(1L, req));
    }

    @Test
    void setPublishedTogglesStatusAndUpdatesReviewer() {
        KnowledgeArticle existing = sampleArticle(1L, "slug-1", "Tiêu đề", "Dạ dày", false);
        when(articleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(articleRepository.save(existing)).thenReturn(existing);

        CmsArticlePublishRequest req = new CmsArticlePublishRequest(true, "BS. Trần Văn B");
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
