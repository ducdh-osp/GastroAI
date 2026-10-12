package vn.gastroai.be.application.triage;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import vn.gastroai.be.application.config.AppRuntimeConfigService;
import vn.gastroai.be.application.config.AppRuntimeConfigSnapshot;
import vn.gastroai.be.domain.triage.TriageAlertEvent;
import vn.gastroai.be.domain.triage.TriageAlertStatusChangedEvent;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TriageAlertPublisherTest {

    private static AppRuntimeConfigService configServiceWithRealtimeAlerts(boolean enabled) {
        AppRuntimeConfigService configService = mock(AppRuntimeConfigService.class);
        when(configService.getConfig()).thenReturn(new AppRuntimeConfigSnapshot(5, null, enabled, null, null));
        return configService;
    }

    @Test
    void publishesEventToAdminTriageAlertTopic() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        TriageAlertPublisher publisher = new TriageAlertPublisher(
                messagingTemplate, configServiceWithRealtimeAlerts(true));

        TriageAlertEvent event = new TriageAlertEvent(
                10L,
                1L,
                "Nguyen Van A",
                "0901234567",
                2L,
                3L,
                "Toi bi dau bung du doi va non ra mau",
                List.of("XUAT_HUYET_TIEU_HOA"),
                "NEW",
                Instant.parse("2026-09-30T00:00:00Z"));

        publisher.publish(event);

        verify(messagingTemplate).convertAndSend("/topic/triage-alerts", event);
    }

    @Test
    void publishesEventEvenWhenSessionAndMessageIdAreNull() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        TriageAlertPublisher publisher = new TriageAlertPublisher(
                messagingTemplate, configServiceWithRealtimeAlerts(true));

        TriageAlertEvent event = new TriageAlertEvent(
                11L,
                1L,
                "Nguyen Van A",
                null,
                null,
                null,
                "Dau bung quan quai keo dai",
                List.of("DAU_BUNG_CAP"),
                "NEW",
                Instant.parse("2026-09-30T00:00:00Z"));

        publisher.publish(event);

        assertEquals(List.of("DAU_BUNG_CAP"), event.matchedGroups());
        verify(messagingTemplate).convertAndSend("/topic/triage-alerts", event);
    }

    @Test
    void publishesStatusChangeToSeparateStatusTopic() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        TriageAlertPublisher publisher = new TriageAlertPublisher(
                messagingTemplate, configServiceWithRealtimeAlerts(true));

        TriageAlertStatusChangedEvent event = new TriageAlertStatusChangedEvent(
                10L,
                "IN_PROGRESS",
                5L,
                "ADMIN",
                null,
                null,
                Instant.parse("2026-09-30T00:05:00Z"));

        publisher.publishStatusChange(event);

        verify(messagingTemplate).convertAndSend("/topic/triage-alerts-status", event);
    }

    @Test
    void doesNotPublishAlertWhenRealtimeAlertsDisabledByAdmin() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        TriageAlertPublisher publisher = new TriageAlertPublisher(
                messagingTemplate, configServiceWithRealtimeAlerts(false));

        TriageAlertEvent event = new TriageAlertEvent(
                10L, 1L, "Nguyen Van A", "0901234567", 2L, 3L,
                "Toi bi dau bung du doi va non ra mau",
                List.of("XUAT_HUYET_TIEU_HOA"), "NEW",
                Instant.parse("2026-09-30T00:00:00Z"));

        publisher.publish(event);

        verify(messagingTemplate, never()).convertAndSend("/topic/triage-alerts", event);
    }

    @Test
    void stillPublishesStatusChangeWhenRealtimeAlertsDisabled() {
        // UC0059 - cong tac chi tat thong bao "co canh bao MOI" (publish), KHONG tat cap nhat
        // trang thai (publishStatusChange) - cac admin dang mo san trang CmsTriageAlertsPage
        // van can thay ngay khi 1 canh bao duoc tiep nhan/xu ly xong du cong tac nay dang tat.
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        TriageAlertPublisher publisher = new TriageAlertPublisher(
                messagingTemplate, configServiceWithRealtimeAlerts(false));

        TriageAlertStatusChangedEvent event = new TriageAlertStatusChangedEvent(
                10L, "IN_PROGRESS", 5L, "ADMIN", null, null,
                Instant.parse("2026-09-30T00:05:00Z"));

        publisher.publishStatusChange(event);

        verify(messagingTemplate).convertAndSend("/topic/triage-alerts-status", event);
    }
}