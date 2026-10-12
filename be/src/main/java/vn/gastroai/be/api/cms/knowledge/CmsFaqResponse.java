package vn.gastroai.be.api.cms.knowledge;

import java.time.Instant;

public record CmsFaqResponse(
        Long id,
        String question,
        String answer,
        String category,
        int sortOrder,
        boolean published,
        Instant createdAt,
        Instant updatedAt
) {
}
