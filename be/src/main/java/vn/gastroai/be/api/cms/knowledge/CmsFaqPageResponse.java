package vn.gastroai.be.api.cms.knowledge;

import java.util.List;

public record CmsFaqPageResponse(
        List<CmsFaqResponse> items,
        long total,
        int page,
        int pageSize
) {
}
