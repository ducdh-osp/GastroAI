package vn.gastroai.be.application.triage;

import java.time.Instant;
import java.util.List;

public record TriageAlertDetailResponse(
        TriageAlertResponse alert,
        List<ActionItem> actions,
        List<MessageItem> messages,
        EmergencyContact emergencyContact) {
    public TriageAlertDetailResponse(TriageAlertResponse alert, List<ActionItem> actions, List<MessageItem> messages) {
        this(alert, actions, messages, null);
    }

    public record EmergencyContact(String name, String phone, String relationship) {}

    public record ActionItem(
            Long id,
            String actionType,
            String actorType,
            Long actorId,
            String detail,
            Instant createdAt) {
    }

    public record MessageItem(
            Long id,
            String senderType,
            Long senderId,
            String content,
            Instant readAt,
            Instant createdAt) {
    }
}
