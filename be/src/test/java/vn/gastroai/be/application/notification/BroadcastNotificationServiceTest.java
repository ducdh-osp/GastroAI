package vn.gastroai.be.application.notification;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import vn.gastroai.be.api.notification.BroadcastAnnouncementRequest;
import vn.gastroai.be.api.notification.BroadcastAnnouncementResponse;
import vn.gastroai.be.domain.notification.AppNotification;
import vn.gastroai.be.domain.notification.BroadcastAnnouncement;
import vn.gastroai.be.domain.notification.NotificationCreatedEvent;
import vn.gastroai.be.infrastructure.persistence.postgres.AppNotificationRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.BroadcastAnnouncementRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BroadcastNotificationServiceTest {

    private final BroadcastAnnouncementRepository broadcastRepository = mock(BroadcastAnnouncementRepository.class);
    private final AppNotificationRepository notificationRepository = mock(AppNotificationRepository.class);
    private final PatientRepository patientRepository = mock(PatientRepository.class);
    private final PatientNotificationSettingService settingService = mock(PatientNotificationSettingService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    private final BroadcastNotificationService service = new BroadcastNotificationService(
            broadcastRepository, notificationRepository, patientRepository, settingService, eventPublisher);

    @Test
    void sendBroadcastCreatesAnnouncementAndDeliversToEligiblePatients() {
        when(patientRepository.findAllVerifiedIds()).thenReturn(List.of(1L, 2L, 3L));
        when(settingService.isSystemBroadcastEnabled(1L)).thenReturn(true);
        when(settingService.isSystemBroadcastEnabled(2L)).thenReturn(false); // Opted out
        when(settingService.isSystemBroadcastEnabled(3L)).thenReturn(true);

        when(broadcastRepository.save(any(BroadcastAnnouncement.class))).thenAnswer(inv -> {
            BroadcastAnnouncement a = inv.getArgument(0);
            if (a.getId() == null) a.setId(100L);
            return a;
        });

        when(notificationRepository.save(any(AppNotification.class))).thenAnswer(inv -> {
            AppNotification n = inv.getArgument(0);
            n.setId(200L);
            return n;
        });

        BroadcastAnnouncementRequest request = new BroadcastAnnouncementRequest(
                "Bao tri he thong",
                "He thong se tam dung vao 02:00 sang chu nhat.",
                "ALL",
                "/faq",
                true
        );

        BroadcastAnnouncementResponse response = service.sendBroadcast(1L, request);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals("Bao tri he thong", response.title());
        assertEquals(2, response.recipientCount()); // Only patient 1 and 3

        verify(notificationRepository, times(2)).save(any(AppNotification.class));
        verify(eventPublisher, times(2)).publishEvent(any(NotificationCreatedEvent.class));
    }

    @Test
    void listBroadcastsReturnsPagedResults() {
        BroadcastAnnouncement a = new BroadcastAnnouncement("Tin tuc", "Noi dung", "ALL", null, 1L);
        a.setId(1L);
        a.setRecipientCount(15);
        when(broadcastRepository.findAllByOrderByCreatedAtDesc(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(a)));

        Page<BroadcastAnnouncementResponse> page = service.listBroadcasts(PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals("Tin tuc", page.getContent().get(0).title());
    }
}
