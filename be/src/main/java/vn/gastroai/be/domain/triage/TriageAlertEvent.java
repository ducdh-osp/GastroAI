package vn.gastroai.be.domain.triage;

import java.time.Instant;
import java.util.List;

public record TriageAlertEvent(
        Long id,
        Long patientId,
        String patientFullName,
        String patientPhone,
        Long sessionId,
        Long messageId,
        String messageContent,
        List<String> matchedGroups,
        String status,
        Instant occurredAt) {
}