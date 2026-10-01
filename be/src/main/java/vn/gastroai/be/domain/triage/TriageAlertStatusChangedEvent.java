package vn.gastroai.be.domain.triage;

import java.time.Instant;

public record TriageAlertStatusChangedEvent(
        Long id,
        String status,
        Long claimedById,
        String claimedByType,
        Instant statusChangedAt) {
}