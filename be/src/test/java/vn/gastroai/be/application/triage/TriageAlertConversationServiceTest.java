package vn.gastroai.be.application.triage;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import vn.gastroai.be.application.notification.AppNotificationService;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.triage.TriageAlert;
import vn.gastroai.be.domain.triage.TriageAlertActionType;
import vn.gastroai.be.domain.triage.TriageAlertActivityEvent;
import vn.gastroai.be.domain.triage.TriageAlertMessage;
import vn.gastroai.be.domain.triage.TriageAlertStatus;
import vn.gastroai.be.infrastructure.persistence.postgres.TriageAlertActionRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.TriageAlertMessageRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.TriageAlertRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TriageAlertConversationServiceTest {

    private final TriageAlertRepository triageAlertRepository = mock(TriageAlertRepository.class);
    private final TriageAlertMessageRepository messageRepository = mock(TriageAlertMessageRepository.class);
    private final TriageAlertActionRepository actionRepository = mock(TriageAlertActionRepository.class);
    private final AppNotificationService appNotificationService = mock(AppNotificationService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    private final TriageAlertConversationService service = new TriageAlertConversationService(
            triageAlertRepository, messageRepository, actionRepository, appNotificationService, eventPublisher);

    private TriageAlert alert(TriageAlertStatus status, Long claimerId, String claimerType) {
        Patient patient = new Patient();
        patient.setId(1L);
        TriageAlert alert = new TriageAlert(patient, null, null, "Non ra mau", null, Instant.now());
        alert.setId(10L);
        alert.setStatus(status);
        alert.setClaimedById(claimerId);
        alert.setClaimedByType(claimerType);
        when(triageAlertRepository.findById(10L)).thenReturn(Optional.of(alert));
        return alert;
    }

    private void saveReturnsWithId() {
        when(messageRepository.save(any())).thenAnswer(invocation -> {
            TriageAlertMessage message = invocation.getArgument(0);
            message.setId(77L);
            return message;
        });
    }

    private static TriageAlertMessage message(String senderType, Long senderId, String content) {
        TriageAlertMessage message = new TriageAlertMessage(10L, senderType, senderId, content);
        message.setId(senderId * 100);
        return message;
    }

    // ---------- nhan vien gui ----------

    @Test
    void staffCannotMessageBeforeClaiming() {
        alert(TriageAlertStatus.NEW, null, null);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.sendStaffMessage(10L, 9L, "DOCTOR", "Ban hay den vien ngay"));

        assertEquals("Cảnh báo id=10 phải được tiếp nhận trước khi nhắn tin cho bệnh nhân",
                exception.getMessage());
        verify(messageRepository, never()).save(any());
        verify(appNotificationService, never()).recordTriageDoctorMessage(any(), any(), any(), any());
    }

    @Test
    void staffWhoDidNotClaimCannotMessage() {
        alert(TriageAlertStatus.IN_PROGRESS, 9L, "DOCTOR");

        assertThrows(IllegalStateException.class,
                () -> service.sendStaffMessage(10L, 5L, "DOCTOR", "Xin chao"));
        verify(messageRepository, never()).save(any());
    }

    @Test
    void staffMessageIsSavedNotifiedAndBroadcast() {
        alert(TriageAlertStatus.IN_PROGRESS, 9L, "DOCTOR");
        saveReturnsWithId();

        TriageAlertConversationService.StaffMessageResult result =
                service.sendStaffMessage(10L, 9L, "DOCTOR", "  Ban hay den vien ngay  ");

        assertEquals(1L, result.patientId());
        assertEquals("Ban hay den vien ngay", result.message().content());
        verify(messageRepository).save(argThat(message ->
                message.getSenderType().equals("DOCTOR")
                        && message.getSenderId().equals(9L)
                        && message.getContent().equals("Ban hay den vien ngay")));
        verify(actionRepository).save(argThat(action ->
                action.getActionType() == TriageAlertActionType.MESSAGE_SENT));
        verify(appNotificationService).recordTriageDoctorMessage(1L, 10L, 77L, "Ban hay den vien ngay");
        verify(eventPublisher).publishEvent(argThat((TriageAlertActivityEvent event) ->
                event.alertId().equals(10L) && event.type().equals("MESSAGE_SENT")));
    }

    @Test
    void adminCanMessageAlertClaimedByDoctor() {
        alert(TriageAlertStatus.IN_PROGRESS, 9L, "DOCTOR");
        saveReturnsWithId();

        service.sendStaffMessage(10L, 1L, "ADMIN", "Nhan vien y te lien he");

        verify(messageRepository).save(argThat(message -> message.getSenderType().equals("ADMIN")));
    }

    @Test
    void staffMessageRejectsBlankContent() {
        alert(TriageAlertStatus.IN_PROGRESS, 9L, "DOCTOR");

        assertThrows(IllegalArgumentException.class, () -> service.sendStaffMessage(10L, 9L, "DOCTOR", "   "));
        assertThrows(IllegalArgumentException.class,
                () -> service.sendStaffMessage(10L, 9L, "DOCTOR", "a".repeat(2001)));
    }

    // ---------- benh nhan ----------

    @Test
    void patientCannotReplyBeforeStaffMessaged() {
        alert(TriageAlertStatus.IN_PROGRESS, 9L, "DOCTOR");
        when(messageRepository.countStaffMessages(10L)).thenReturn(0L);

        assertThrows(IllegalStateException.class, () -> service.replyAsPatient(1L, 10L, "alo"));
        verify(messageRepository, never()).save(any());
    }

    @Test
    void patientCannotReplyToResolvedAlert() {
        alert(TriageAlertStatus.RESOLVED, 9L, "DOCTOR");
        when(messageRepository.countStaffMessages(10L)).thenReturn(1L);

        assertThrows(IllegalStateException.class, () -> service.replyAsPatient(1L, 10L, "alo"));
        verify(messageRepository, never()).save(any());
    }

    @Test
    void patientReplyIsSavedAndBroadcast() {
        alert(TriageAlertStatus.IN_PROGRESS, 9L, "DOCTOR");
        when(messageRepository.countStaffMessages(10L)).thenReturn(1L);
        saveReturnsWithId();

        TriageAlertConversationService.PatientMessageItem item = service.replyAsPatient(1L, 10L, "Toi dang di");

        assertTrue(item.mine());
        assertEquals("Bạn", item.senderLabel());
        verify(actionRepository).save(argThat(action ->
                action.getActionType() == TriageAlertActionType.PATIENT_REPLIED));
        verify(eventPublisher).publishEvent(argThat((TriageAlertActivityEvent event) ->
                event.type().equals("PATIENT_MESSAGE")));
    }

    @Test
    void patientCannotSeeAnotherPatientsAlert() {
        alert(TriageAlertStatus.IN_PROGRESS, 9L, "DOCTOR");

        assertThrows(ResourceNotFoundException.class, () -> service.getForPatient(2L, 10L));
        assertThrows(ResourceNotFoundException.class, () -> service.replyAsPatient(2L, 10L, "alo"));
        verify(messageRepository, never()).markStaffMessagesRead(anyLong(), any());
    }

    @Test
    void patientOpeningConversationMarksStaffMessagesReadAndHidesStaffIdentity() {
        alert(TriageAlertStatus.IN_PROGRESS, 9L, "DOCTOR");
        when(messageRepository.markStaffMessagesRead(eq(10L), any())).thenReturn(1);
        when(messageRepository.findByAlertIdOrderByCreatedAtAscIdAsc(10L)).thenReturn(List.of(
                message("DOCTOR", 9L, "Ban hay den vien"),
                message("ADMIN", 1L, "Nhan vien goi lai"),
                message("PATIENT", 1L, "Vang")));

        TriageAlertConversationService.PatientConversationView view = service.getForPatient(1L, 10L);

        assertTrue(view.canReply());
        assertEquals(List.of("Bác sĩ", "Nhân viên y tế", "Bạn"),
                view.messages().stream().map(TriageAlertConversationService.PatientMessageItem::senderLabel).toList());
        assertFalse(view.messages().get(0).mine());
        verify(actionRepository).save(argThat(action ->
                action.getActionType() == TriageAlertActionType.PATIENT_READ));
        verify(eventPublisher).publishEvent(argThat((TriageAlertActivityEvent event) ->
                event.type().equals("PATIENT_READ")));
    }

    @Test
    void patientOpeningWithNothingNewDoesNotRecordRead() {
        alert(TriageAlertStatus.RESOLVED, 9L, "DOCTOR");
        when(messageRepository.markStaffMessagesRead(eq(10L), any())).thenReturn(0);
        when(messageRepository.findByAlertIdOrderByCreatedAtAscIdAsc(10L))
                .thenReturn(List.of(message("DOCTOR", 9L, "Ban hay den vien")));

        TriageAlertConversationService.PatientConversationView view = service.getForPatient(1L, 10L);

        assertFalse(view.canReply());
        verify(actionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(TriageAlertActivityEvent.class));
    }
}
