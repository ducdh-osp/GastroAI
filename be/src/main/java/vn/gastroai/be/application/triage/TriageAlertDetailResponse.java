package vn.gastroai.be.application.triage;

import java.time.Instant;
import java.util.List;

public record TriageAlertDetailResponse(
        TriageAlertResponse alert,
        List<ActionItem> actions,
        List<MessageItem> messages) {

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
