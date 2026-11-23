package vn.gastroai.be.application.triage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import vn.gastroai.be.application.notification.AppNotificationService;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.patient.MedicalProfile;
import vn.gastroai.be.domain.triage.TriageAlert;
import vn.gastroai.be.domain.triage.TriageAlertActionType;
import vn.gastroai.be.domain.triage.TriageAlertActivityEvent;
import vn.gastroai.be.domain.triage.TriageAlertEvent;
import vn.gastroai.be.domain.triage.TriageAlertOutcome;
import vn.gastroai.be.domain.triage.TriageAlertStatus;
import vn.gastroai.be.domain.triage.TriageAlertStatusChangedEvent;
import vn.gastroai.be.domain.triage.TriageContactResult;
import vn.gastroai.be.domain.triage.TriagePatientResponse;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.MedicalProfileRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.TriageAlertActionRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.TriageAlertMessageRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.TriageAlertRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class TriageAlertServiceTest {

    private final TriageAlertRepository triageAlertRepository = mock(TriageAlertRepository.class);
    private final TriageAlertActionRepository actionRepository = mock(TriageAlertActionRepository.class);
    private final TriageAlertMessageRepository messageRepository = mock(TriageAlertMessageRepository.class);
    private final PatientRepository patientRepository = mock(PatientRepository.class);
    private final MedicalProfileRepository medicalProfileRepository = mock(MedicalProfileRepository.class);
    private final AppNotificationService appNotificationService = mock(AppNotificationService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final TriageAlertService service = new TriageAlertService(
            triageAlertRepository, actionRepository, messageRepository, patientRepository, medicalProfileRepository,
            appNotificationService, eventPublisher, objectMapper);

    private Patient patient(Long id, String fullName, String phone) {
        Patient patient = new Patient();
        patient.setId(id);
        patient.setFullName(fullName);
        patient.setPhone(phone);
        return patient;
    }

    /** Ca id=10 cua benh nhan #1 o trang thai cho truoc; findById luon tra ve chinh doi tuong nay. */
    private TriageAlert alertIn(TriageAlertStatus status) {
        TriageAlert alert = new TriageAlert(patient(1L, "Nguyen Van A", "0901234567"), null,
                "Cau hoi khan cap", null, Instant.parse("2026-09-30T00:00:00Z"));
        alert.setId(10L);
        alert.setStatus(status);
        when(triageAlertRepository.findById(10L)).thenReturn(Optional.of(alert));
        return alert;
    }

    private TriageAlert interventionAlert(Long claimerId, String claimerType) {
        TriageAlert alert = alertIn(TriageAlertStatus.NEEDS_INTERVENTION);
        alert.setNeedsInterventionAt(Instant.parse("2026-09-30T00:03:00Z"));
        alert.setClaimedById(claimerId);
        alert.setClaimedByType(claimerType);
        return alert;
    }

    private void verifyAction(TriageAlertActionType type) {
        verify(actionRepository).save(argThat(action -> action.getActionType() == type));
    }

    private static TriageAlertMessageRepository.AlertMessageCount messageCount(Long alertId, long total) {
        return new TriageAlertMessageRepository.AlertMessageCount() {
            public Long getAlertId() { return alertId; }
            public long getTotal() { return total; }
        };
    }

    private static TriageAlertRepository.PatientAlertCount patientCount(Long patientId, long total) {
        return new TriageAlertRepository.PatientAlertCount() {
            public Long getPatientId() { return patientId; }
            public long getTotal() { return total; }
        };
    }

    // ---------- createAndPublish ----------

    @Test
    void createAndPublishSavesAlertRecordsTimelineAndPublishesEnrichedEvent() {
        Patient patient = patient(1L, "Nguyen Van A", "0901234567");
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(triageAlertRepository.save(any())).thenAnswer(invocation -> {
            TriageAlert saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        Long alertId = service.createAndPublish(
                1L, null,
                "Toi bi dau bung du doi va non ra mau",
                List.of("XUAT_HUYET_TIEU_HOA"));

        assertEquals(100L, alertId);
        verify(triageAlertRepository).save(argThat(alert ->
                alert.getPatient().equals(patient)
                        && alert.getStatus() == TriageAlertStatus.AUTO_HANDLING
                        && alert.getMessageContent().equals("Toi bi dau bung du doi va non ra mau")
                        && alert.getRepeatCount() == 0));
        verifyAction(TriageAlertActionType.CREATED);
        // SCR-AD-13 buoc 1: hoi tham benh nhan ngay va ghi len timeline.
        verify(appNotificationService).recordTriageCheckIn(1L, 100L, false);
        verifyAction(TriageAlertActionType.CHECK_IN_SENT);
        verify(eventPublisher).publishEvent(argThat((TriageAlertEvent event) ->
                event.id().equals(100L)
                        && event.patientId().equals(1L)
                        && event.patientFullName().equals("Nguyen Van A")
                        && event.patientPhone().equals("0901234567")
                        && event.status().equals("AUTO_HANDLING")
                        && event.matchedGroups().equals(List.of("XUAT_HUYET_TIEU_HOA"))));
    }

    @Test
    void createAndPublishThrowsWhenPatientNotFound() {
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.createAndPublish(
                99L, null, "Cau hoi", List.of()));

        verify(triageAlertRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(TriageAlertEvent.class));
    }

    @Test
    void createAndPublishMergesIntoOpenAlertKeepingOriginalMessageAndDoesNotPopNewAlert() {
        Patient patient = patient(1L, "Nguyen Van A", "0901234567");
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        TriageAlert existing = new TriageAlert(patient, 9L, "Non ra mau",
                "[\"XUAT_HUYET_TIEU_HOA\"]", Instant.now().minusSeconds(60));
        existing.setId(50L);
        when(triageAlertRepository.findLatestOpenForPatient(eq(1L), any()))
                .thenReturn(List.of(existing));
        when(triageAlertRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Long alertId = service.createAndPublish(
                1L, null, "Dau bung du doi hon nhieu roi", List.of("DAU_BUNG_CAP_TINH"));

        assertEquals(50L, alertId);
        assertEquals("Non ra mau", existing.getMessageContent());
        assertEquals("Dau bung du doi hon nhieu roi", existing.getLastMessageContent());
        assertEquals(1, existing.getRepeatCount());
        assertEquals("[\"XUAT_HUYET_TIEU_HOA\",\"DAU_BUNG_CAP_TINH\"]", existing.getMatchedGroups());
        assertEquals(9L, existing.getMessageId());
        verifyAction(TriageAlertActionType.REPEATED);
        verify(eventPublisher).publishEvent(argThat((TriageAlertActivityEvent event) ->
                event.alertId().equals(50L) && event.type().equals("REPEATED")));
        verify(eventPublisher, never()).publishEvent(any(TriageAlertEvent.class));
        // Gop vao ca dang mo thi khong hoi tham lan nua.
        verify(appNotificationService, never()).recordTriageCheckIn(any(), any(), anyBoolean());
    }

    @Test
    void createAndPublishMergesIntoInterventionAlertEvenAfter10Minutes() {
        Patient patient = patient(1L, "Nguyen Van A", "0901234567");
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        TriageAlert existing = new TriageAlert(patient, 9L, "Non ra mau",
                null, Instant.now().minus(Duration.ofMinutes(10)));
        existing.setId(50L);
        existing.setStatus(TriageAlertStatus.NEEDS_INTERVENTION);
        existing.setClaimedById(9L);
        existing.setClaimedByType("ADMIN");
        when(triageAlertRepository.findLatestOpenForPatient(eq(1L), any()))
                .thenReturn(List.of(existing));
        when(triageAlertRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Long alertId = service.createAndPublish(1L, null, "Van con non ra mau", List.of());

        assertEquals(50L, alertId);
        assertEquals(TriageAlertStatus.NEEDS_INTERVENTION, existing.getStatus());
        assertEquals(1, existing.getRepeatCount());
        assertEquals("Van con non ra mau", existing.getLastMessageContent());
        verify(triageAlertRepository, never()).save(argThat(alert -> alert.getId() == null));
        verify(eventPublisher, never()).publishEvent(any(TriageAlertEvent.class));
    }

    @Test
    void createAndPublishMergesIntoFalseAlarmWithin30MinutesAndKeepsItResolved() {
        Patient patient = patient(1L, "Nguyen Van A", "0901234567");
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        TriageAlert falseAlarm = new TriageAlert(patient, null, "Non ra mau",
                null, Instant.now().minus(Duration.ofMinutes(40)));
        falseAlarm.setId(60L);
        falseAlarm.setStatus(TriageAlertStatus.RESOLVED);
        falseAlarm.setOutcome(TriageAlertOutcome.FALSE_ALARM);
        falseAlarm.setResolvedAt(Instant.now().minus(Duration.ofMinutes(10)));
        when(triageAlertRepository.findFirstByPatient_IdOrderByOccurredAtDescIdDesc(1L))
                .thenReturn(Optional.of(falseAlarm));
        when(triageAlertRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Long alertId = service.createAndPublish(1L, null, "Lai non ra mau", List.of("XUAT_HUYET_TIEU_HOA"));

        assertEquals(60L, alertId);
        assertEquals(TriageAlertStatus.RESOLVED, falseAlarm.getStatus());
        assertEquals(1, falseAlarm.getRepeatCount());
        assertEquals("Lai non ra mau", falseAlarm.getLastMessageContent());
        verifyAction(TriageAlertActionType.REPEATED_AFTER_FALSE_ALARM);
        verify(eventPublisher).publishEvent(argThat((TriageAlertActivityEvent event) ->
                event.alertId().equals(60L) && event.type().equals("REPEATED_AFTER_FALSE_ALARM")));
        verify(eventPublisher, never()).publishEvent(any(TriageAlertEvent.class));
    }

    @Test
    void createAndPublishCreatesNewAlertWhenFalseAlarmOlderThan30Minutes() {
        Patient patient = patient(1L, "Nguyen Van A", "0901234567");
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        TriageAlert falseAlarm = new TriageAlert(patient, null, "Non ra mau",
                null, Instant.now().minus(Duration.ofHours(2)));
        falseAlarm.setId(60L);
        falseAlarm.setStatus(TriageAlertStatus.RESOLVED);
        falseAlarm.setOutcome(TriageAlertOutcome.FALSE_ALARM);
        falseAlarm.setResolvedAt(Instant.now().minus(Duration.ofMinutes(31)));
        when(triageAlertRepository.findFirstByPatient_IdOrderByOccurredAtDescIdDesc(1L))
                .thenReturn(Optional.of(falseAlarm));
        when(triageAlertRepository.save(any())).thenAnswer(invocation -> {
            TriageAlert saved = invocation.getArgument(0);
            if (saved.getId() == null) saved.setId(100L);
            return saved;
        });

        Long alertId = service.createAndPublish(1L, null, "Lai non ra mau", List.of());

        assertEquals(100L, alertId);
        assertEquals(0, falseAlarm.getRepeatCount());
        verifyAction(TriageAlertActionType.CREATED);
        verify(eventPublisher).publishEvent(argThat((TriageAlertEvent event) -> event.id().equals(100L)));
    }

    @Test
    void createAndPublishCreatesNewAlertWhenLatestAlertWasHandledNormally() {
        Patient patient = patient(1L, "Nguyen Van A", "0901234567");
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        TriageAlert handled = new TriageAlert(patient, null, "Non ra mau", null, Instant.now());
        handled.setId(60L);
        handled.setStatus(TriageAlertStatus.RESOLVED);
        handled.setOutcome(TriageAlertOutcome.HANDLED);
        handled.setResolvedAt(Instant.now().minus(Duration.ofMinutes(1)));
        when(triageAlertRepository.findFirstByPatient_IdOrderByOccurredAtDescIdDesc(1L))
                .thenReturn(Optional.of(handled));
        when(triageAlertRepository.save(any())).thenAnswer(invocation -> {
            TriageAlert saved = invocation.getArgument(0);
            if (saved.getId() == null) saved.setId(100L);
            return saved;
        });

        assertEquals(100L, service.createAndPublish(1L, null, "Lai non ra mau", List.of()));
    }

    // ---------- linkConversation ----------

    @Test
    void linkConversationSetsRealIdsAndRepublishesEvent() {
        Patient patient = patient(1L, "Nguyen Van A", "0901234567");
        TriageAlert alert = new TriageAlert(patient, null, "Toi bi dau bung du doi qua",
                "[\"DAU_BUNG_CAP_TINH\"]", Instant.parse("2026-09-30T00:00:00Z"));
        alert.setId(100L);
        when(triageAlertRepository.findById(100L)).thenReturn(Optional.of(alert));
        when(triageAlertRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.linkConversation(100L, 9L);

        verify(triageAlertRepository).save(argThat(saved ->
                saved.getMessageId().equals(9L)));
        verify(eventPublisher).publishEvent(argThat((TriageAlertEvent event) ->
                event.id().equals(100L)
                        && event.messageId().equals(9L)
                        && event.messageContent().equals("Toi bi dau bung du doi qua")));
    }

    @Test
    void linkConversationKeepsExistingMessageOfMergedAlert() {
        Patient patient = patient(1L, "Nguyen Van A", "0901234567");
        TriageAlert alert = new TriageAlert(patient, 4L, "Non ra mau", null, Instant.now());
        alert.setId(100L);
        alert.setRepeatCount(2);
        when(triageAlertRepository.findById(100L)).thenReturn(Optional.of(alert));

        service.linkConversation(100L, 9L);

        assertEquals(4L, alert.getMessageId());
        verify(triageAlertRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(TriageAlertEvent.class));
    }

    @Test
    void linkConversationDoesNothingWhenAlertNoLongerExists() {
        when(triageAlertRepository.findById(999L)).thenReturn(Optional.empty());

            service.linkConversation(999L, 9L);

        verify(triageAlertRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(TriageAlertEvent.class));
    }

    // ---------- bo hen gio: buoc 3 ----------

    @Test
    void escalateMovesSilentAlertToInterventionAndNotifiesAllAdmins() {
        TriageAlert alert = alertIn(TriageAlertStatus.AUTO_HANDLING);
        when(triageAlertRepository.escalateIfAuto(eq(10L), any())).thenAnswer(invocation -> {
            alert.setStatus(TriageAlertStatus.NEEDS_INTERVENTION);
            alert.setNeedsInterventionAt(invocation.getArgument(1));
            return 1;
        });

        Optional<TriageAlertResponse> result = service.escalateIfStillSilent(10L, Duration.ofMinutes(3));

        assertTrue(result.isPresent());
        assertEquals("NEEDS_INTERVENTION", result.get().status());
        verify(actionRepository).save(argThat(action ->
                action.getActionType() == TriageAlertActionType.ESCALATED
                        && "SYSTEM".equals(action.getActorType())
                        && "Bệnh nhân không phản hồi sau 3 phút".equals(action.getDetail())));
        verify(appNotificationService).recordTriageNeedsIntervention(
                10L, "Nguyen Van A", "Bệnh nhân không phản hồi sau 3 phút", false);
        verify(eventPublisher).publishEvent(argThat((TriageAlertStatusChangedEvent event) ->
                event.status().equals("NEEDS_INTERVENTION")));
    }

    @Test
    void escalateDoesNothingWhenPatientAlreadyAnswered() {
        alertIn(TriageAlertStatus.RESOLVED);
        when(triageAlertRepository.escalateIfAuto(eq(10L), any())).thenReturn(0);

        assertTrue(service.escalateIfStillSilent(10L, Duration.ofMinutes(3)).isEmpty());

        verify(actionRepository, never()).save(any());
        verify(appNotificationService, never()).recordTriageNeedsIntervention(any(), any(), any(), anyBoolean());
    }

    // ---------- bo hen gio: buoc 4 failsafe ----------

    @Test
    void failsafeRealertsAdminsAndRemindsPatientButKeepsAlertOpen() {
        TriageAlert alert = interventionAlert(null, null);
        when(triageAlertRepository.markFailsafeIfDue(eq(10L), any())).thenAnswer(invocation -> {
            alert.setFailsafeAt(invocation.getArgument(1));
            return 1;
        });
        MedicalProfile profile = new MedicalProfile();
        profile.setEmergencyContactPhone("0909000111");
        when(medicalProfileRepository.findByPatientId(1L)).thenReturn(Optional.of(profile));

        Optional<TriageAlertResponse> result = service.runFailsafeIfUnacknowledged(10L, Duration.ofMinutes(5));

        assertTrue(result.isPresent());
        assertEquals("NEEDS_INTERVENTION", result.get().status());
        verify(actionRepository).save(argThat(action ->
                action.getActionType() == TriageAlertActionType.FAILSAFE
                        && action.getDetail().startsWith("Không có Admin nào xác nhận trong 5 phút")
                        && !action.getDetail().contains("chưa khai")));
        verify(appNotificationService).recordTriageNeedsIntervention(
                eq(10L), eq("Nguyen Van A"), any(), eq(true));
        verify(appNotificationService).recordTriageCheckIn(1L, 10L, true);
        verify(triageAlertRepository, never()).closeIfOpen(any(), any(), any(), any(), any(), any());
    }

    @Test
    void failsafeNotesMissingEmergencyContact() {
        interventionAlert(null, null);
        when(triageAlertRepository.markFailsafeIfDue(eq(10L), any())).thenReturn(1);
        when(medicalProfileRepository.findByPatientId(1L)).thenReturn(Optional.empty());

        service.runFailsafeIfUnacknowledged(10L, Duration.ofMinutes(5));

        verify(actionRepository).save(argThat(action ->
                action.getActionType() == TriageAlertActionType.FAILSAFE
                        && action.getDetail().endsWith("Bệnh nhân chưa khai liên hệ khẩn cấp")));
    }

    @Test
    void failsafeSkipsWhenAdminAlreadyActed() {
        interventionAlert(5L, "ADMIN");
        when(triageAlertRepository.markFailsafeIfDue(eq(10L), any())).thenReturn(0);

        assertTrue(service.runFailsafeIfUnacknowledged(10L, Duration.ofMinutes(5)).isEmpty());

        verify(appNotificationService, never()).recordTriageCheckIn(any(), any(), anyBoolean());
        verify(actionRepository, never()).save(any());
    }

    // ---------- benh nhan tra loi "Ban co on khong?" ----------

    @Test
    void patientSafeClosesAlertAsPatientSafe() {
        TriageAlert alert = alertIn(TriageAlertStatus.AUTO_HANDLING);
        when(triageAlertRepository.closeIfOpen(eq(10L), eq(1L), eq("PATIENT"),
                eq(TriageAlertOutcome.PATIENT_SAFE), eq("Bệnh nhân báo an toàn"), any())).thenAnswer(invocation -> {
            alert.setStatus(TriageAlertStatus.RESOLVED);
            return 1;
        });

        service.respondToCheckIn(1L, 10L, TriagePatientResponse.SAFE);

        verify(triageAlertRepository).recordPatientResponse(eq(10L), eq(TriagePatientResponse.SAFE), any());
        verify(appNotificationService).markTriageAlertNotificationsRead(10L);
        verifyAction(TriageAlertActionType.PATIENT_SAFE);
        verify(eventPublisher).publishEvent(argThat((TriageAlertStatusChangedEvent event) ->
                event.status().equals("RESOLVED") && "PATIENT".equals(event.resolvedByType())));
    }

    @Test
    void answeringAnAlreadyClosedAlertChangesNothing() {
        alertIn(TriageAlertStatus.RESOLVED);

        TriageAlertService.CheckInResult result =
                service.respondToCheckIn(1L, 10L, TriagePatientResponse.NEED_HELP);

        assertTrue(result.alreadyClosed());
        verify(triageAlertRepository, never()).recordPatientResponse(any(), any(), any());
        verify(appNotificationService, never()).recordTriageNeedsIntervention(any(), any(), any(), anyBoolean());
    }

    @Test
    void patientSafeAfterEscalationStillClosesWithNote() {
        interventionAlert(5L, "ADMIN");
        when(triageAlertRepository.closeIfOpen(any(), any(), any(), any(), any(), any())).thenReturn(1);

        service.respondToCheckIn(1L, 10L, TriagePatientResponse.SAFE);

        verify(triageAlertRepository).closeIfOpen(eq(10L), eq(1L), eq("PATIENT"),
                eq(TriageAlertOutcome.PATIENT_SAFE), eq("Bệnh nhân báo an toàn (sau khi đã chuyển Admin)"), any());
    }

    @Test
    void patientNeedHelpEscalatesImmediately() {
        TriageAlert alert = alertIn(TriageAlertStatus.AUTO_HANDLING);
        when(triageAlertRepository.escalateIfAuto(eq(10L), any())).thenAnswer(invocation -> {
            alert.setStatus(TriageAlertStatus.NEEDS_INTERVENTION);
            return 1;
        });

        service.respondToCheckIn(1L, 10L, TriagePatientResponse.NEED_HELP);

        verifyAction(TriageAlertActionType.PATIENT_NEEDS_HELP);
        verify(actionRepository).save(argThat(action ->
                action.getActionType() == TriageAlertActionType.ESCALATED
                        && "PATIENT".equals(action.getActorType())));
        verify(appNotificationService).recordTriageNeedsIntervention(
                10L, "Nguyen Van A", "Bệnh nhân bấm Cần hỗ trợ", false);
    }

    @Test
    void patientNeedHelpOnInterventionAlertRealertsAdmins() {
        interventionAlert(null, null);

        service.respondToCheckIn(1L, 10L, TriagePatientResponse.NEED_HELP);

        verify(triageAlertRepository, never()).escalateIfAuto(any(), any());
        verify(appNotificationService).recordTriageNeedsIntervention(
                10L, "Nguyen Van A", "Bệnh nhân bấm Cần hỗ trợ", true);
        verify(eventPublisher).publishEvent(argThat((TriageAlertActivityEvent event) ->
                event.type().equals("PATIENT_NEEDS_HELP")));
    }

    @Test
    void patientNeedHelpRacingWithTimerStillRealerts() {
        // Doc thay "dang tu xu ly" nhung bo hen gio vua chuyen truoc -> escalateIfAuto tra 0.
        alertIn(TriageAlertStatus.AUTO_HANDLING);
        when(triageAlertRepository.escalateIfAuto(eq(10L), any())).thenReturn(0);

        service.respondToCheckIn(1L, 10L, TriagePatientResponse.NEED_HELP);

        verify(appNotificationService).recordTriageNeedsIntervention(
                10L, "Nguyen Van A", "Bệnh nhân bấm Cần hỗ trợ", true);
    }

    @Test
    void patientAnswerOnClosedAlertIsIgnored() {
        alertIn(TriageAlertStatus.RESOLVED);

        service.respondToCheckIn(1L, 10L, TriagePatientResponse.NEED_HELP);

        verify(triageAlertRepository, never()).recordPatientResponse(any(), any(), any());
        verify(actionRepository, never()).save(any());
    }

    @Test
    void patientCannotAnswerSomeoneElsesAlert() {
        alertIn(TriageAlertStatus.AUTO_HANDLING);

        assertThrows(vn.gastroai.be.application.support.ResourceNotFoundException.class,
                () -> service.respondToCheckIn(2L, 10L, TriagePatientResponse.SAFE));
        verify(triageAlertRepository, never()).recordPatientResponse(any(), any(), any());
    }

    // ---------- Admin: "Toi dang lien he" ----------

    @Test
    void claimOnAutoAlertIsEarlyInterventionAndBroadcastsSameTimestampUsedInDb() {
        Patient patient = patient(1L, "Nguyen Van A", "0901234567");
        TriageAlert alert = new TriageAlert(patient, null, "Cau hoi khan cap",
                "[\"DAU_BUNG_CAP_TINH\"]", Instant.parse("2026-09-30T00:00:00Z"));
        alert.setId(10L);
        when(triageAlertRepository.findById(10L)).thenReturn(Optional.of(alert));
        when(triageAlertRepository.claimIfOpen(eq(10L), eq(5L), eq("ADMIN"), any())).thenAnswer(invocation -> {
            Instant now = invocation.getArgument(3);
            alert.setStatus(TriageAlertStatus.NEEDS_INTERVENTION);
            alert.setClaimedById(5L);
            alert.setClaimedByType("ADMIN");
            alert.setClaimedAt(now);
            alert.setAcknowledgedAt(now);
            return 1;
        });

        TriageAlertResponse response = service.claim(10L, 5L, "ADMIN");

        assertEquals("NEEDS_INTERVENTION", response.status());
        assertEquals(5L, response.claimedById());
        assertEquals(List.of("DAU_BUNG_CAP_TINH"), response.matchedGroups());
        verify(actionRepository).save(argThat(action ->
                action.getActionType() == TriageAlertActionType.CLAIMED
                        && "Can thiệp sớm, trước mốc tự động".equals(action.getDetail())));
        verify(eventPublisher).publishEvent(argThat((TriageAlertStatusChangedEvent event) ->
                event.id().equals(10L)
                        && event.status().equals("NEEDS_INTERVENTION")
                        && event.statusChangedAt().equals(alert.getClaimedAt())));
    }

    @Test
    void claimThrowsWhenAlertAlreadyResolved() {
        alertIn(TriageAlertStatus.RESOLVED);
        when(triageAlertRepository.claimIfOpen(eq(10L), any(), any(), any())).thenReturn(0);

        assertThrows(IllegalStateException.class, () -> service.claim(10L, 5L, "ADMIN"));

        verify(eventPublisher, never()).publishEvent(any(TriageAlertStatusChangedEvent.class));
    }

    @Test
    void claimReturnsCurrentStateWhenSameClaimerRepeatsRequest() {
        interventionAlert(5L, "ADMIN");
        when(triageAlertRepository.claimIfOpen(eq(10L), eq(5L), eq("ADMIN"), any())).thenReturn(0);

        TriageAlertResponse response = service.claim(10L, 5L, "ADMIN");

        assertEquals("NEEDS_INTERVENTION", response.status());
        verify(eventPublisher, never()).publishEvent(any(TriageAlertStatusChangedEvent.class));
    }

    @Test
    void claimThrowsWhenAnotherAdminIsAlreadyContacting() {
        interventionAlert(9L, "ADMIN");
        when(triageAlertRepository.claimIfOpen(eq(10L), eq(5L), eq("ADMIN"), any())).thenReturn(0);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.claim(10L, 5L, "ADMIN"));

        assertEquals("Admin #9 đang liên hệ cho ca này", exception.getMessage());
    }

    @Test
    void releaseByClaimerClearsClaimAndRestartsFailsafe() {
        TriageAlert alert = interventionAlert(5L, "ADMIN");
        when(triageAlertRepository.releaseIfClaimedBy(eq(10L), eq(5L), eq("ADMIN"), any()))
                .thenAnswer(invocation -> {
                    alert.setClaimedById(null);
                    alert.setClaimedByType(null);
                    alert.setAcknowledgedAt(null);
                    return 1;
                });

        TriageAlertResponse response = service.release(10L, 5L, "ADMIN");

        assertEquals("NEEDS_INTERVENTION", response.status());
        assertNull(response.claimedById());
        assertNull(response.acknowledgedAt());
        verify(actionRepository).save(argThat(action ->
                action.getActionType() == TriageAlertActionType.RELEASED && action.getDetail() == null));
        verify(triageAlertRepository, never()).releaseAsAdmin(any(), any());
    }

    @Test
    void releaseByAdminOnBehalfNotesOriginalClaimer() {
        interventionAlert(9L, "ADMIN");
        when(triageAlertRepository.releaseIfClaimedBy(eq(10L), eq(5L), eq("ADMIN"), any())).thenReturn(0);
        when(triageAlertRepository.releaseAsAdmin(eq(10L), any())).thenReturn(1);

        service.release(10L, 5L, "ADMIN");

        verify(actionRepository).save(argThat(action ->
                action.getActionType() == TriageAlertActionType.RELEASED
                        && "Admin trả lại hộ, người tiếp nhận: ADMIN #9".equals(action.getDetail())));
    }

    @Test
    void releaseThrowsWhenNobodyClaimed() {
        interventionAlert(null, null);

        assertThrows(IllegalStateException.class, () -> service.release(10L, 5L, "ADMIN"));
        verify(triageAlertRepository, never()).releaseIfClaimedBy(any(), any(), any(), any());
    }

    // ---------- Admin: ghi ket qua lien he ----------

    @Test
    void recordContactAcknowledgesAndWritesLabelWithNote() {
        interventionAlert(null, null);
        when(triageAlertRepository.acknowledgeIfOpen(eq(10L), any())).thenReturn(1);

        service.recordContact(10L, 5L, "ADMIN", TriageContactResult.RELATIVE_REACHED, "  Chong se ve nha ngay ");

        verify(triageAlertRepository).acknowledgeIfOpen(eq(10L), any());
        verify(actionRepository).save(argThat(action ->
                action.getActionType() == TriageAlertActionType.CONTACT_RECORDED
                        && "Đã gọi người thân - người thân sẽ kiểm tra · Chong se ve nha ngay"
                                .equals(action.getDetail())));
        verify(eventPublisher).publishEvent(argThat((TriageAlertActivityEvent event) ->
                event.type().equals("CONTACT_RECORDED")));
    }

    @Test
    void recordContactOnAutoAlertMovesItToIntervention() {
        TriageAlert alert = alertIn(TriageAlertStatus.AUTO_HANDLING);
        when(triageAlertRepository.acknowledgeIfOpen(eq(10L), any())).thenAnswer(invocation -> {
            alert.setStatus(TriageAlertStatus.NEEDS_INTERVENTION);
            return 1;
        });

        service.recordContact(10L, 5L, "ADMIN", TriageContactResult.CALLED_EMERGENCY, null);

        verify(eventPublisher).publishEvent(argThat((TriageAlertStatusChangedEvent event) ->
                event.status().equals("NEEDS_INTERVENTION")));
    }

    @Test
    void recordContactRequiresResultAndOpenAlert() {
        interventionAlert(null, null);
        assertThrows(IllegalArgumentException.class,
                () -> service.recordContact(10L, 5L, "ADMIN", null, null));

        alertIn(TriageAlertStatus.RESOLVED);
        assertThrows(IllegalStateException.class,
                () -> service.recordContact(10L, 5L, "ADMIN", TriageContactResult.PATIENT_REACHED, null));
        verify(triageAlertRepository, never()).acknowledgeIfOpen(any(), any());
    }

    // ---------- Admin: dong ca ----------

    @Test
    void resolveThrowsWhenNothingWasDoneYet() {
        interventionAlert(5L, "ADMIN");
        when(actionRepository.countByAlertIdAndActionType(10L, TriageAlertActionType.CONTACT_RECORDED)).thenReturn(0L);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.resolve(10L, 5L, "ADMIN", null));

        assertEquals("Cần ghi kết quả liên hệ trước khi đóng ca", exception.getMessage());
        verify(triageAlertRepository, never()).closeIfOpen(any(), any(), any(), any(), any(), any());
    }

    @Test
    void resolveAfterContactRecordedMarksHandledByAnyAdmin() {
        TriageAlert alert = interventionAlert(9L, "ADMIN");
        when(actionRepository.countByAlertIdAndActionType(10L, TriageAlertActionType.CONTACT_RECORDED)).thenReturn(1L);
        when(triageAlertRepository.closeIfOpen(eq(10L), eq(5L), eq("ADMIN"), eq(TriageAlertOutcome.HANDLED),
                eq("Nguoi than da dua di vien"), any())).thenAnswer(invocation -> {
            alert.setStatus(TriageAlertStatus.RESOLVED);
            alert.setOutcome(TriageAlertOutcome.HANDLED);
            alert.setResolvedAt(invocation.getArgument(5));
            return 1;
        });

        TriageAlertResponse response = service.resolve(10L, 5L, "ADMIN", "  Nguoi than da dua di vien ");
        verify(appNotificationService).markTriageAlertNotificationsRead(10L);

        assertEquals("RESOLVED", response.status());
        assertEquals("HANDLED", response.outcome());
        verifyAction(TriageAlertActionType.RESOLVED);
        verify(eventPublisher).publishEvent(argThat((TriageAlertStatusChangedEvent event) ->
                event.status().equals("RESOLVED") && event.statusChangedAt().equals(alert.getResolvedAt())));
    }

    @Test
    void resolveThrowsWhenAlertAlreadyResolved() {
        alertIn(TriageAlertStatus.RESOLVED);

        assertThrows(IllegalStateException.class, () -> service.resolve(10L, 5L, "ADMIN", null));
    }

    @Test
    void resolveRejectsTooLongNote() {
        interventionAlert(5L, "ADMIN");

        assertThrows(IllegalArgumentException.class,
                () -> service.resolve(10L, 5L, "ADMIN", "x".repeat(1001)));
    }

    @Test
    void markFalseAlarmRequiresReason() {
        interventionAlert(5L, "ADMIN");

        assertThrows(IllegalArgumentException.class, () -> service.markFalseAlarm(10L, 5L, "ADMIN", " a "));
        verify(triageAlertRepository, never()).closeIfOpen(any(), any(), any(), any(), any(), any());
    }

    @Test
    void markFalseAlarmWorksWhileSystemIsStillHandling() {
        // Admin thay ngay la bao nham ("hoi xua em tung non ra mau") thi dong luon, khong cho 3 phut.
        alertIn(TriageAlertStatus.AUTO_HANDLING);
        when(triageAlertRepository.closeIfOpen(any(), any(), any(), any(), any(), any())).thenReturn(1);

        service.markFalseAlarm(10L, 5L, "ADMIN", "Benh nhan ke chuyen cu");

        verify(triageAlertRepository).closeIfOpen(eq(10L), eq(5L), eq("ADMIN"),
                eq(TriageAlertOutcome.FALSE_ALARM), eq("Benh nhan ke chuyen cu"), any());
        verify(actionRepository).save(argThat(action ->
                action.getActionType() == TriageAlertActionType.FALSE_ALARM
                        && "Benh nhan ke chuyen cu".equals(action.getDetail())));
    }

    @Test
    void closeThrowsWhenSomeoneElseClosedConcurrently() {
        interventionAlert(5L, "ADMIN");
        when(triageAlertRepository.closeIfOpen(any(), any(), any(), any(), any(), any())).thenReturn(0);

        assertThrows(IllegalStateException.class, () -> service.markFalseAlarm(10L, 5L, "ADMIN", "Spam"));
    }

    // ---------- list ----------

    @Test
    void listOpenQueriesBothOpenStatusesAndFillsCounters() {
        Patient patient = patient(1L, "Nguyen Van A", "0901234567");
        TriageAlert alert = new TriageAlert(patient, 3L, "Cau hoi khan cap",
                "[\"XUAT_HUYET_TIEU_HOA\",\"DAU_BUNG_CAP_TINH\"]",
                Instant.parse("2026-09-30T00:00:00Z"));
        alert.setId(10L);
        Page<TriageAlert> page = new PageImpl<>(List.of(alert));
        when(triageAlertRepository.findPageByStatuses(
                eq(Set.of(TriageAlertStatus.AUTO_HANDLING, TriageAlertStatus.NEEDS_INTERVENTION)), any()))
                .thenReturn(page);
        when(messageRepository.countUnreadPatientMessagesByAlert(List.of(10L)))
                .thenReturn(List.of(messageCount(10L, 2)));
        when(messageRepository.countStaffMessagesByAlert(List.of(10L)))
                .thenReturn(List.of(messageCount(10L, 3)));
        when(triageAlertRepository.countFalseAlarmsByPatient(Set.of(1L)))
                .thenReturn(List.of(patientCount(1L, 1)));

        Page<TriageAlertResponse> result = service.list(null, 0, 20);

        assertEquals(1, result.getTotalElements());
        TriageAlertResponse response = result.getContent().get(0);
        assertEquals(10L, response.id());
        assertEquals("Nguyen Van A", response.patientFullName());
        assertEquals(List.of("XUAT_HUYET_TIEU_HOA", "DAU_BUNG_CAP_TINH"), response.matchedGroups());
        assertEquals(2, response.unreadPatientMessages());
        assertEquals(3, response.staffMessageCount());
        assertEquals(1, response.patientFalseAlarmCount());
    }

    @Test
    void listRejectsUnknownView() {
        assertThrows(IllegalArgumentException.class, () -> service.list("XYZ", 0, 20));
        verify(triageAlertRepository, never()).findPageByStatuses(any(), any());
    }

    @Test
    void listClosedQueriesResolvedOnly() {
        when(triageAlertRepository.findPageByStatuses(eq(Set.of(TriageAlertStatus.RESOLVED)), any()))
                .thenReturn(Page.empty());

        assertEquals(0, service.list("closed", 0, 20).getTotalElements());
    }

    @Test
    void listInterventionAndAutoTabsQuerySingleStatus() {
        when(triageAlertRepository.findPageByStatuses(any(), any())).thenReturn(Page.empty());

        service.list("intervention", 0, 20);
        service.list("AUTO", 0, 20);

        verify(triageAlertRepository).findPageByStatuses(eq(Set.of(TriageAlertStatus.NEEDS_INTERVENTION)), any());
        verify(triageAlertRepository).findPageByStatuses(eq(Set.of(TriageAlertStatus.AUTO_HANDLING)), any());
    }

    // ---------- chi tiet ----------

    @Test
    void getDetailByClaimerMarksPatientMessagesRead() {
        interventionAlert(7L, "ADMIN");

        TriageAlertDetailResponse detail = service.getDetail(10L, 7L, "ADMIN");

        assertEquals(10L, detail.alert().id());
        verify(messageRepository).markPatientMessagesRead(eq(10L), any());
    }

    @Test
    void getDetailByOtherAdminKeepsMessagesUnread() {
        interventionAlert(7L, "ADMIN");

        service.getDetail(10L, 5L, "ADMIN");

        verify(messageRepository, never()).markPatientMessagesRead(any(), any());
    }

    @Test
    void everyAdminSeesEmergencyContactOnceInterventionIsNeeded() {
        // SCR-AD-13: bao toi TOAN BO Admin dang truc - ai cung phai thay so de goi.
        interventionAlert(7L, "ADMIN");
        MedicalProfile profile = new MedicalProfile();
        profile.setEmergencyContactName("Nguyen Van B");
        profile.setEmergencyContactPhone("0901234567");
        profile.setEmergencyContactRelationship("Cha");
        when(medicalProfileRepository.findByPatientId(1L)).thenReturn(Optional.of(profile));

        TriageAlertDetailResponse claimer = service.getDetail(10L, 7L, "ADMIN");
        TriageAlertDetailResponse other = service.getDetail(10L, 8L, "ADMIN");

        assertEquals("0901234567", claimer.emergencyContact().phone());
        assertEquals("0901234567", other.emergencyContact().phone());
    }

    @Test
    void emergencyContactHiddenWhileSystemIsStillHandling() {
        alertIn(TriageAlertStatus.AUTO_HANDLING);

        TriageAlertDetailResponse detail = service.getDetail(10L, 7L, "ADMIN");

        assertNull(detail.emergencyContact());
        verify(medicalProfileRepository, never()).findByPatientId(any());
    }

    @Test
    void getDetailComparesBothIdAndAccountType() {
        // Admin #7 va Bac si #7 la 2 tai khoan khac nhau (2 bang MySQL rieng).
        interventionAlert(7L, "DOCTOR");

        service.getDetail(10L, 7L, "ADMIN");

        verify(messageRepository, never()).markPatientMessagesRead(any(), any());
    }

    @Test
    void getDetailThrowsNotFoundForUnknownAlert() {
        when(triageAlertRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(vn.gastroai.be.application.support.ResourceNotFoundException.class,
                () -> service.getDetail(404L, 7L, "ADMIN"));
        verify(messageRepository, never()).markPatientMessagesRead(any(), any());
    }
}
