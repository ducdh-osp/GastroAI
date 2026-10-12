package vn.gastroai.be.application.triage;

import java.time.Instant;
import java.util.List;


public record TriageAlertResponse(
        Long id,
        Long patientId,
        String patientFullName,
        String patientPhone,
        Long sessionId,
        Long messageId,
        String messageContent,
        List<String> matchedGroups,
        String status,
        Long claimedById,
        String claimedByType,
        Instant claimedAt,
        Instant resolvedAt,
        Long resolvedById,
        String resolvedByType,
        Instant occurredAt) {
}