package vn.gastroai.be.domain.triage;

import java.time.Instant;

/**
 * Bao CMS "canh bao nay vua co thay doi" (topic /topic/triage-alerts-activity).
 * type: REPEATED, REPEATED_AFTER_FALSE_ALARM, MESSAGE_SENT, PATIENT_MESSAGE, PATIENT_READ.
 */
public record TriageAlertActivityEvent(
        Long alertId,
        String type,
        Instant occurredAt) {
}
