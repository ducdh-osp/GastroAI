package vn.gastroai.be.api.notification;

import java.time.Instant;

public record BroadcastAnnouncementResponse(
        Long id,
        String title,
        String message,
        String targetGroup,
        String actionUrl,
        Long createdByAdminId,
        int recipientCount,
        Instant createdAt
) {
}
