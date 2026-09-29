package vn.gastroai.be.application.triage;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import vn.gastroai.be.domain.triage.TriageAlertEvent;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * UC0036 - xac nhan TriageAlertPublisher day dung topic va dung nguyen payload sang
 * SimpMessagingTemplate, khong lam bien dang du lieu truoc khi broadcast cho admin (UC0067).
 */
class TriageAlertPublisherTest {

    @Test
    void publishesEventToAdminTriageAlertTopic() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        TriageAlertPublisher publisher = new TriageAlertPublisher(messagingTemplate);

        TriageAlertEvent event = new TriageAlertEvent(
                1L,
                2L,
                3L,
                "Toi bi dau bung du doi va non ra mau",
                List.of("XUAT_HUYET_TIEU_HOA"),
                Instant.parse("2026-09-30T00:00:00Z"));

        publisher.publish(event);

        verify(messagingTemplate).convertAndSend("/topic/triage-alerts", event);
    }

    @Test
    void publishesEventEvenWhenSessionAndMessageIdAreNull() {
        // Luong streaming (SSE) chua luu lich su chat nen sessionId/messageId co the null -
        // publisher khong duoc phep loai bo hay bao loi trong truong hop nay.
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        TriageAlertPublisher publisher = new TriageAlertPublisher(messagingTemplate);

        TriageAlertEvent event = new TriageAlertEvent(
                1L,
                null,
                null,
                "Dau bung quan quai keo dai",
                List.of("DAU_BUNG_CAP"),
                Instant.parse("2026-09-30T00:00:00Z"));

        publisher.publish(event);

        assertEquals(List.of("DAU_BUNG_CAP"), event.matchedGroups());
        verify(messagingTemplate).convertAndSend("/topic/triage-alerts", event);
    }
}