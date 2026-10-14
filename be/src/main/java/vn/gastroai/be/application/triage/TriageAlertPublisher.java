package vn.gastroai.be.application.triage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import vn.gastroai.be.application.config.AppRuntimeConfigService;
import vn.gastroai.be.domain.triage.TriageAlertEvent;
import vn.gastroai.be.domain.triage.TriageAlertStatusChangedEvent;

@Service
public class TriageAlertPublisher {
    static final String DESTINATION = "/topic/triage-alerts";

    static final String DESTINATION_STATUS = "/topic/triage-alerts-status";

    private static final Logger log = LoggerFactory.getLogger(TriageAlertPublisher.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final AppRuntimeConfigService appRuntimeConfigService;

    public TriageAlertPublisher(SimpMessagingTemplate messagingTemplate,
            AppRuntimeConfigService appRuntimeConfigService) {
        this.messagingTemplate = messagingTemplate;
        this.appRuntimeConfigService = appRuntimeConfigService;
    }

    /**
     * UC0059 - admin co the tam tat kenh thong bao realtime (vd khi dang bao tri/debug) ma
     * khong anh huong den viec canh bao van duoc tao/luu trong DB - CmsTriageAlertsPage van thay
     * day du khi tai lai trang, chi mat phan "day ngay qua WebSocket".
     */
    public void publish(TriageAlertEvent event) {
        if (!appRuntimeConfigService.getConfig().triageRealtimeAlertsEnabled()) {
            return;
        }
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