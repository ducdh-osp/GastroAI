package vn.gastroai.be.api.notification;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String type,
        String title,
        String message,
        String sourceType,
        Long sourceId,
        String actionUrl,
        Instant createdAt,
        Instant readAt,
        String scheduledTime) {

    public NotificationResponse(
            Long id,
            String type,
            String title,
            String message,
            String sourceType,
            Long sourceId,
            String actionUrl,
            Instant createdAt,
            Instant readAt) {
        this(id, type, title, message, sourceType, sourceId, actionUrl, createdAt, readAt, null);
    }
}
