package vn.gastroai.be.application.triage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import vn.gastroai.be.domain.triage.TriageAlertEvent;
import vn.gastroai.be.domain.triage.TriageAlertStatusChangedEvent;

@Service
public class TriageAlertPublisher {
    static final String DESTINATION = "/topic/triage-alerts";

    static final String DESTINATION_STATUS = "/topic/triage-alerts-status";

    private static final Logger log = LoggerFactory.getLogger(TriageAlertPublisher.class);

    private final SimpMessagingTemplate messagingTemplate;

    public TriageAlertPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
    public void publish(TriageAlertEvent event) {
        try {
            messagingTemplate.convertAndSend(DESTINATION, event);
        } catch (Exception exception) {
            log.error("Khong the day canh bao Triage qua WebSocket cho patientId={}: {}",
                    event.patientId(), exception.getMessage(), exception);
        }
    }

    public void publishStatusChange(TriageAlertStatusChangedEvent event) {
        try {
            messagingTemplate.convertAndSend(DESTINATION_STATUS, event);
        } catch (Exception exception) {
            log.error("Khong the day cap nhat trang thai canh bao Triage id={}: {}",
                    event.id(), exception.getMessage(), exception);
        }
    }
}