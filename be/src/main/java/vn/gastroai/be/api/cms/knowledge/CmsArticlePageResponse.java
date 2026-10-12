package vn.gastroai.be.api.cms.knowledge;

import org.springframework.data.domain.Page;

import java.util.List;

/** UC0065 - Danh sach phan trang bai viet CMS. */
public record CmsArticlePageResponse(
        List<CmsArticleSummaryResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static CmsArticlePageResponse from(Page<CmsArticleSummaryResponse> page) {
        return new CmsArticlePageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
