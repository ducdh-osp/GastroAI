package vn.gastroai.be.domain.notification;

import vn.gastroai.be.api.notification.NotificationResponse;

public record NotificationCreatedEvent(
        NotificationRecipientType recipientType,
        Long recipientId,
        NotificationResponse notification) {
}
