package vn.gastroai.be.api.document;

import java.util.List;

public record DocumentPageResponse(
        List<DocumentSummaryResponse> items,
        long total,
        int page,
        int pageSize,
        DocumentStats stats
) {
}
