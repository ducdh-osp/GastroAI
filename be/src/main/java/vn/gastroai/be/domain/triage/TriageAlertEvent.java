package vn.gastroai.be.domain.triage;

import java.time.Instant;
import java.util.List;

public record TriageAlertEvent(
        Long patientId,
        Long sessionId,
        Long messageId,
        String messageSnippet,
        List<String> matchedGroups,
        Instant occurredAt) {
}
