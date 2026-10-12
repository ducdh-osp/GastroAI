package vn.gastroai.be.application.triage;

import java.time.Instant;

public record TriageKeywordResponse(
        Long id,
        String groupCode,
        String groupLabel,
        String keyword,
        Instant createdAt,
        Instant updatedAt) {
}
