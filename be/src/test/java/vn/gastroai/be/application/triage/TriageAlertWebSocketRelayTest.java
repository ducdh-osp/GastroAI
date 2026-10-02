package vn.gastroai.be.application.triage;

import org.junit.jupiter.api.Test;
import vn.gastroai.be.domain.triage.TriageAlertEvent;
import vn.gastroai.be.domain.triage.TriageAlertStatusChangedEvent;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TriageAlertWebSocketRelayTest {

    private final TriageAlertPublisher triageAlertPublisher = mock(TriageAlertPublisher.class);
    private final TriageAlertWebSocketRelay relay = new TriageAlertWebSocketRelay(triageAlertPublisher);

    @Test
    void onAlertForwardsEventToPublisher() {
        TriageAlertEvent event = new TriageAlertEvent(
                100L, 1L, "Nguyen Van A", "0901234567", null, null,
                "Toi bi dau bung du doi", List.of("DAU_BUNG_CAP_TINH"), "NEW",
                Instant.parse("2026-09-30T00:00:00Z"));

        relay.onAlert(event);

        verify(triageAlertPublisher).publish(event);
    }

    @Test
    void onStatusChangeForwardsEventToPublisher() {
        TriageAlertStatusChangedEvent event = new TriageAlertStatusChangedEvent(
                10L, "IN_PROGRESS", 5L, "ADMIN", null, null, Instant.parse("2026-09-30T00:05:00Z"));

        relay.onStatusChange(event);

        verify(triageAlertPublisher).publishStatusChange(event);
    }
}