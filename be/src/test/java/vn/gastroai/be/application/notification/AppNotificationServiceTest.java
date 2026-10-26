package vn.gastroai.be.application.notification;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import vn.gastroai.be.domain.notification.AppNotification;
import vn.gastroai.be.domain.notification.NotificationCreatedEvent;
import vn.gastroai.be.domain.notification.NotificationRecipientType;
import vn.gastroai.be.domain.notification.NotificationState;
import vn.gastroai.be.domain.notification.NotificationType;
import vn.gastroai.be.domain.triage.TriageAlertEvent;
import vn.gastroai.be.infrastructure.persistence.postgres.AppNotificationRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.NotificationStateRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppNotificationServiceTest {

    private final AppNotificationRepository notificationRepository = mock(AppNotificationRepository.class);
    private final NotificationStateRepository stateRepository = mock(NotificationStateRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final AppNotificationService service = new AppNotificationService(
            notificationRepository, stateRepository, eventPublisher);

    @Test
    void dailyReminderIsDeduplicated() {
        when(notificationRepository.findByDedupeKey("key")).thenReturn(Optional.of(patientNotification(1L)));

        assertTrue(service.createJournalReminder(1L, "message", "/food-diary", "key").isEmpty());

        verify(notificationRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void reminderIsPersistedBeforeRealtimeEventIsPublished() {
        when(notificationRepository.findByDedupeKey("key")).thenReturn(Optional.empty());
        when(notificationRepository.save(any(AppNotification.class))).thenAnswer(invocation -> {
            AppNotification notification = invocation.getArgument(0);
            notification.setId(5L);
            return notification;
        });

        assertTrue(service.createJournalReminder(1L, "message", "/food-diary", "key").isPresent());

        verify(notificationRepository).save(any(AppNotification.class));
        verify(eventPublisher).publishEvent(any(NotificationCreatedEvent.class));
    }

    @Test
    void medicationReminderUsesDoseSpecificNotificationTypeAndSource() {
        when(notificationRepository.findByDedupeKey("med-key")).thenReturn(Optional.empty());
        when(notificationRepository.save(any(AppNotification.class))).thenAnswer(invocation -> {
            AppNotification notification = invocation.getArgument(0);
            notification.setId(6L);
            return notification;
        });

        assertTrue(service.createMedicationReminder(
                1L, 9L, "Đến giờ uống thuốc", "message", "med-key").isPresent());

        org.mockito.ArgumentCaptor<AppNotification> notification =
                org.mockito.ArgumentCaptor.forClass(AppNotification.class);
        verify(notificationRepository).save(notification.capture());
        assertTrue(notification.getValue().getNotificationType() == NotificationType.MEDICATION_REMINDER);
        assertTrue(notification.getValue().getSourceId().equals(9L));
        assertTrue("/medications".equals(notification.getValue().getActionUrl()));
    }

    @Test
    void failedLoginCreatesPatientSystemNotificationWithSecurityDetails() {
        when(notificationRepository.save(any(AppNotification.class))).thenAnswer(invocation -> {
            AppNotification notification = invocation.getArgument(0);
            notification.setId(7L);
            return notification;
        });

        service.createFailedLoginAlert(1L, "Chrome on Windows", "203.0.113.10", false);

        org.mockito.ArgumentCaptor<AppNotification> notification =
                org.mockito.ArgumentCaptor.forClass(AppNotification.class);
        verify(notificationRepository).save(notification.capture());
        AppNotification saved = notification.getValue();
        assertEquals(NotificationRecipientType.PATIENT, saved.getRecipientType());
        assertEquals(1L, saved.getRecipientId());
        assertEquals(NotificationType.SYSTEM, saved.getNotificationType());
        assertEquals("LOGIN_SECURITY", saved.getSourceType());
        assertEquals("/login-history", saved.getActionUrl());
        assertTrue(saved.getMessage().contains("Chrome on Windows"));
        assertTrue(saved.getMessage().contains("203.0.113.10"));
        verify(eventPublisher).publishEvent(any(NotificationCreatedEvent.class));
    }

    @Test
    void patientCannotMarkAnotherPatientsNotificationRead() {
        AppNotification notification = patientNotification(2L);
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(notification));

        assertThrows(AccessDeniedException.class,
                () -> service.markRead(5L, NotificationRecipientType.PATIENT, 1L));

        verify(stateRepository, never()).save(any());
    }

    @Test
    void deleteCreatesOnlyRecipientSpecificSoftDeleteState() {
        AppNotification notification = patientNotification(1L);
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(notification));
        when(stateRepository.findByNotificationIdAndRecipientTypeAndRecipientId(
                5L, NotificationRecipientType.PATIENT, 1L)).thenReturn(Optional.empty());

        service.delete(5L, NotificationRecipientType.PATIENT, 1L);

        org.mockito.ArgumentCaptor<NotificationState> state =
                org.mockito.ArgumentCaptor.forClass(NotificationState.class);
        verify(stateRepository).save(state.capture());
        assertNotNull(state.getValue().getDeletedAt());
    }

    @Test
    void readNotificationCanBeMarkedUnreadAgain() {
        AppNotification notification = patientNotification(1L);
        NotificationState state = new NotificationState(
                notification, NotificationRecipientType.PATIENT, 1L);
        state.setReadAt(Instant.now());
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(notification));
        when(stateRepository.findByNotificationIdAndRecipientTypeAndRecipientId(
                5L, NotificationRecipientType.PATIENT, 1L)).thenReturn(Optional.of(state));

        service.markUnread(5L, NotificationRecipientType.PATIENT, 1L);

        assertNull(state.getReadAt());
        verify(stateRepository).save(state);
    }

    @Test
    void triageEventCreatesAdminAuditNotification() {
        when(notificationRepository.findByDedupeKey("TRIAGE_ALERT:9")).thenReturn(Optional.empty());
        when(notificationRepository.save(any(AppNotification.class))).thenAnswer(invocation -> {
            AppNotification notification = invocation.getArgument(0);
            notification.setId(10L);
            return notification;
        });
        TriageAlertEvent event = new TriageAlertEvent(
                9L, 1L, "Nguyen Van A", "0901234567", null, null,
                "Đau bụng dữ dội", List.of("DAU_BUNG_CAP_TINH"), "NEW", Instant.now());

        assertTrue(service.recordTriageAlert(event).isPresent());

        verify(notificationRepository).save(any(AppNotification.class));
        verify(eventPublisher).publishEvent(any(NotificationCreatedEvent.class));
    }

    private AppNotification patientNotification(Long patientId) {
        AppNotification notification = new AppNotification(
                NotificationRecipientType.PATIENT, patientId, NotificationType.JOURNAL_REMINDER,
                "title", "message", "JOURNAL", null, "/food-diary", "key");
        notification.setId(5L);
        return notification;
    }
}
