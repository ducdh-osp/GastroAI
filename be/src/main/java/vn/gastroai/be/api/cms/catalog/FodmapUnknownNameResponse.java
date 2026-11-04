package vn.gastroai.be.api.cms.catalog;

import java.time.Instant;
import vn.gastroai.be.domain.catalog.FodmapUnknownName;

public record FodmapUnknownNameResponse(
        Long id,
        String name,
        int hitCount,
        Instant firstSeenAt,
        Instant lastSeenAt
) {
    public static FodmapUnknownNameResponse from(FodmapUnknownName unknown) {
        return new FodmapUnknownNameResponse(
                unknown.getId(),
                unknown.getSampleName(),
                unknown.getHitCount(),
                unknown.getFirstSeenAt(),
                unknown.getLastSeenAt()
        );
    }
}