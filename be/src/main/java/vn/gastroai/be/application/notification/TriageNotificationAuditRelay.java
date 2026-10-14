package vn.gastroai.be.application.notification;

import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import vn.gastroai.be.domain.triage.TriageAlertEvent;

@Component
public class TriageNotificationAuditRelay {

    private final AppNotificationService notificationService;

    public TriageNotificationAuditRelay(AppNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @EventListener
    public void onAlert(TriageAlertEvent event) {
        // Audit và triage alert cùng transaction: chỉ phát WebSocket AFTER_COMMIT khi cả hai
        // đã được lưu thành công, tránh có cảnh báo đã gửi nhưng không còn dấu vết trong DB.
        notificationService.recordTriageAlert(event);
    }
}
