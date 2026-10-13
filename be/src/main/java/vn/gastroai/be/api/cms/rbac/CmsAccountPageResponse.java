package vn.gastroai.be.api.cms.rbac;

import java.util.List;

public record CmsAccountPageResponse(
        List<CmsAccountResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
