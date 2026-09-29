package vn.gastroai.be.application.triage;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import vn.gastroai.be.domain.triage.TriageAlertEvent;

@Service
public class TriageAlertPublisher {
    static final String DESTINATION = "/topic/triage-alerts";

    private final SimpMessagingTemplate messagingTemplate;

    public TriageAlertPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publish(TriageAlertEvent event) {
        messagingTemplate.convertAndSend(DESTINATION, event);
    }
    
}
