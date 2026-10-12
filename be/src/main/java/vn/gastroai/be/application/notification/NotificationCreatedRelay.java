package vn.gastroai.be.application.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.gastroai.be.domain.notification.NotificationCreatedEvent;
import vn.gastroai.be.domain.notification.NotificationRecipientType;

@Component
public class NotificationCreatedRelay {

    private static final Logger log = LoggerFactory.getLogger(NotificationCreatedRelay.class);
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationCreatedRelay(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNotificationCreated(NotificationCreatedEvent event) {
        String destination = event.recipientType() == NotificationRecipientType.PATIENT
                ? "/topic/notifications/patient/" + event.recipientId()
                : "/topic/notifications/admin";
        try {
            messagingTemplate.convertAndSend(destination, event.notification());
        } catch (Exception exception) {
            log.error("Khong the day notification qua WebSocket destination={}: {}",
                    destination, exception.getMessage(), exception);
        }
    }
}
