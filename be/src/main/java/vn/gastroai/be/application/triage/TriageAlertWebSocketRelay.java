package vn.gastroai.be.application.triage;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.gastroai.be.domain.triage.TriageAlertEvent;
import vn.gastroai.be.domain.triage.TriageAlertStatusChangedEvent;


@Component
public class TriageAlertWebSocketRelay {

    private final TriageAlertPublisher triageAlertPublisher;

    public TriageAlertWebSocketRelay(TriageAlertPublisher triageAlertPublisher) {
        this.triageAlertPublisher = triageAlertPublisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAlert(TriageAlertEvent event) {
        triageAlertPublisher.publish(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onStatusChange(TriageAlertStatusChangedEvent event) {
        triageAlertPublisher.publishStatusChange(event);
    }
}