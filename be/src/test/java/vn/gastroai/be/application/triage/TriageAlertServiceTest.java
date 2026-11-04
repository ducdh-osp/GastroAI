package vn.gastroai.be.application.triage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.triage.TriageAlert;
import vn.gastroai.be.domain.triage.TriageAlertActionType;
import vn.gastroai.be.domain.triage.TriageAlertActivityEvent;
import vn.gastroai.be.domain.triage.TriageAlertEvent;
import vn.gastroai.be.domain.triage.TriageAlertOutcome;
import vn.gastroai.be.domain.triage.TriageAlertStatus;
import vn.gastroai.be.domain.triage.TriageAlertStatusChangedEvent;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.TriageAlertActionRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.TriageAlertMessageRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.TriageAlertRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final TriageAlertService service = new TriageAlertService(
            triageAlertRepository, actionRepository, messageRepository, patientRepository,
            eventPublisher, objectMapper);

    private Patient patient(Long id, String fullName, String phone) {
        Patient patient = new Patient();
        patient.setId(id);
        patient.setFullName(fullName);
        patient.setPhone(phone);
        return patient;
    }

    private TriageAlert inProgressAlert(Long claimerId, String claimerType) {
        TriageAlert alert = new TriageAlert(patient(1L, "Nguyen Van A", "0901234567"), null, null,
                "Cau hoi khan cap", null, Instant.parse("2026-09-30T00:00:00Z"));
        alert.setId(10L);
        alert.setStatus(TriageAlertStatus.IN_PROGRESS);
        alert.setClaimedById(claimerId);
        alert.setClaimedByType(claimerType);
        when(triageAlertRepository.findById(10L)).thenReturn(Optional.of(alert));
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
                1L, null, null,
                "Toi bi dau bung du doi va non ra mau",
                List.of("XUAT_HUYET_TIEU_HOA"));

        assertEquals(100L, alertId);
        verify(triageAlertRepository).save(argThat(alert ->
                alert.getPatient().equals(patient)
                        && alert.getStatus() == TriageAlertStatus.NEW
                        && alert.getMessageContent().equals("Toi bi dau bung du doi va non ra mau")
                        && alert.getRepeatCount() == 0));
        verifyAction(TriageAlertActionType.CREATED);
        verify(eventPublisher).publishEvent(argThat((TriageAlertEvent event) ->
                event.id().equals(100L)
                        && event.patientId().equals(1L)
                        && event.patientFullName().equals("Nguyen Van A")
                        && event.patientPhone().equals("0901234567")
                        && event.status().equals("NEW")
                        && event.matchedGroups().equals(List.of("XUAT_HUYET_TIEU_HOA"))));
    }

    @Test
    void createAndPublishThrowsWhenPatientNotFound() {
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.createAndPublish(
                99L, null, null, "Cau hoi", List.of()));

        verify(triageAlertRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(TriageAlertEvent.class));
    }

    @Test
    void createAndPublishMergesIntoOpenAlertKeepingOriginalMessageAndDoesNotPopNewAlert() {
        Patient patient = patient(1L, "Nguyen Van A", "0901234567");
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        TriageAlert existing = new TriageAlert(patient, 7L, 9L, "Non ra mau",
                "[\"XUAT_HUYET_TIEU_HOA\"]", Instant.now().minusSeconds(60));
        existing.setId(50L);
        when(triageAlertRepository.findRecentOpenForPatient(eq(1L), any(), any()))
                .thenReturn(List.of(existing));
        when(triageAlertRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Long alertId = service.createAndPublish(
                1L, null, null, "Dau bung du doi hon nhieu roi", List.of("DAU_BUNG_CAP_TINH"));

        assertEquals(50L, alertId);
        assertEquals("Non ra mau", existing.getMessageContent());
        assertEquals("Dau bung du doi hon nhieu roi", existing.getLastMessageContent());
        assertEquals(1, existing.getRepeatCount());
        assertEquals("[\"XUAT_HUYET_TIEU_HOA\",\"DAU_BUNG_CAP_TINH\"]", existing.getMatchedGroups());
        assertEquals(7L, existing.getSessionId());
        verifyAction(TriageAlertActionType.REPEATED);
        verify(eventPublisher).publishEvent(argThat((TriageAlertActivityEvent event) ->
                event.alertId().equals(50L) && event.type().equals("REPEATED")));
        verify(eventPublisher, never()).publishEvent(any(TriageAlertEvent.class));
    }

    @Test
    void createAndPublishMergesIntoFalseAlarmWithin30MinutesAndKeepsItResolved() {
        Patient patient = patient(1L, "Nguyen Van A", "0901234567");
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        TriageAlert falseAlarm = new TriageAlert(patient, null, null, "Non ra mau",
                null, Instant.now().minus(Duration.ofMinutes(40)));
        falseAlarm.setId(60L);
        falseAlarm.setStatus(TriageAlertStatus.RESOLVED);
        falseAlarm.setOutcome(TriageAlertOutcome.FALSE_ALARM);
        falseAlarm.setResolvedAt(Instant.now().minus(Duration.ofMinutes(10)));
        when(triageAlertRepository.findFirstByPatient_IdOrderByOccurredAtDescIdDesc(1L))
                .thenReturn(Optional.of(falseAlarm));
        when(triageAlertRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Long alertId = service.createAndPublish(1L, null, null, "Lai non ra mau", List.of("XUAT_HUYET_TIEU_HOA"));

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
        TriageAlert falseAlarm = new TriageAlert(patient, null, null, "Non ra mau",
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

        Long alertId = service.createAndPublish(1L, null, null, "Lai non ra mau", List.of());

        assertEquals(100L, alertId);
        assertEquals(0, falseAlarm.getRepeatCount());
        verifyAction(TriageAlertActionType.CREATED);
        verify(eventPublisher).publishEvent(argThat((TriageAlertEvent event) -> event.id().equals(100L)));
    }

    @Test
    void createAndPublishCreatesNewAlertWhenLatestAlertWasHandledNormally() {
        Patient patient = patient(1L, "Nguyen Van A", "0901234567");
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        TriageAlert handled = new TriageAlert(patient, null, null, "Non ra mau", null, Instant.now());
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

        assertEquals(100L, service.createAndPublish(1L, null, null, "Lai non ra mau", List.of()));
    }

    // ---------- linkConversation ----------

    @Test
    void linkConversationSetsRealIdsAndRepublishesEvent() {
        Patient patient = patient(1L, "Nguyen Van A", "0901234567");
        TriageAlert alert = new TriageAlert(patient, null, null, "Toi bi dau bung du doi qua",
                "[\"DAU_BUNG_CAP_TINH\"]", Instant.parse("2026-09-30T00:00:00Z"));
        alert.setId(100L);
        when(triageAlertRepository.findById(100L)).thenReturn(Optional.of(alert));
        when(triageAlertRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.linkConversation(100L, 7L, 9L);

        verify(triageAlertRepository).save(argThat(saved ->
                saved.getSessionId().equals(7L) && saved.getMessageId().equals(9L)));
        verify(eventPublisher).publishEvent(argThat((TriageAlertEvent event) ->
                event.id().equals(100L)
                        && event.sessionId().equals(7L)
                        && event.messageId().equals(9L)
                        && event.messageContent().equals("Toi bi dau bung du doi qua")));
    }

    @Test
    void linkConversationKeepsExistingSessionOfMergedAlert() {
        Patient patient = patient(1L, "Nguyen Van A", "0901234567");
        TriageAlert alert = new TriageAlert(patient, 3L, 4L, "Non ra mau", null, Instant.now());
        alert.setId(100L);
        alert.setRepeatCount(2);
        when(triageAlertRepository.findById(100L)).thenReturn(Optional.of(alert));

        service.linkConversation(100L, 7L, 9L);

        assertEquals(3L, alert.getSessionId());
        assertEquals(4L, alert.getMessageId());
        verify(triageAlertRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(TriageAlertEvent.class));
    }

    @Test
    void linkConversationDoesNothingWhenAlertNoLongerExists() {
        when(triageAlertRepository.findById(999L)).thenReturn(Optional.empty());

        service.linkConversation(999L, 7L, 9L);

        verify(triageAlertRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(TriageAlertEvent.class));
    }

    // ---------- claim ----------

    @Test
    void claimTransitionsToInProgressRecordsTimelineAndBroadcastsSameTimestampUsedInDb() {
        Patient patient = patient(1L, "Nguyen Van A", "0901234567");
        TriageAlert alert = new TriageAlert(patient, null, null, "Cau hoi khan cap",
                "[\"DAU_BUNG_CAP_TINH\"]", Instant.parse("2026-09-30T00:00:00Z"));
        alert.setId(10L);
        when(triageAlertRepository.findById(10L)).thenReturn(Optional.of(alert));
        when(triageAlertRepository.claimIfNew(eq(10L), eq(5L), eq("ADMIN"), any())).thenAnswer(invocation -> {
            Instant now = invocation.getArgument(3);
            alert.setStatus(TriageAlertStatus.IN_PROGRESS);
            alert.setClaimedById(5L);
            alert.setClaimedByType("ADMIN");
            alert.setClaimedAt(now);
            return 1;
        });

        TriageAlertResponse response = service.claim(10L, 5L, "ADMIN");

        assertEquals("IN_PROGRESS", response.status());
        assertEquals(5L, response.claimedById());
        assertEquals(List.of("DAU_BUNG_CAP_TINH"), response.matchedGroups());
        verifyAction(TriageAlertActionType.CLAIMED);
        verify(eventPublisher).publishEvent(argThat((TriageAlertStatusChangedEvent event) ->
                event.id().equals(10L)
                        && event.status().equals("IN_PROGRESS")
                        && event.resolvedById() == null
                        && event.statusChangedAt().equals(alert.getClaimedAt())));
    }

    @Test
    void claimThrowsWhenAlertAlreadyResolved() {
        TriageAlert alert = inProgressAlert(null, null);
        alert.setStatus(TriageAlertStatus.RESOLVED);
        when(triageAlertRepository.claimIfNew(eq(10L), any(), any(), any())).thenReturn(0);

        assertThrows(IllegalStateException.class, () -> service.claim(10L, 5L, "ADMIN"));

        verify(eventPublisher, never()).publishEvent(any(TriageAlertStatusChangedEvent.class));
    }

    @Test
    void claimReturnsCurrentStateWhenSameClaimerRepeatsRequest() {
        inProgressAlert(5L, "ADMIN");
        when(triageAlertRepository.claimIfNew(eq(10L), eq(5L), eq("ADMIN"), any())).thenReturn(0);

        TriageAlertResponse response = service.claim(10L, 5L, "ADMIN");

        assertEquals("IN_PROGRESS", response.status());
        verify(eventPublisher, never()).publishEvent(any(TriageAlertStatusChangedEvent.class));
    }

    @Test
    void claimThrowsWhenAnotherStaffAlreadyClaimedConcurrently() {
        inProgressAlert(9L, "DOCTOR");
        when(triageAlertRepository.claimIfNew(eq(10L), eq(5L), eq("ADMIN"), any())).thenReturn(0);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.claim(10L, 5L, "ADMIN"));

        assertEquals("Cảnh báo đã được Bác sĩ #9 tiếp nhận", exception.getMessage());
    }

    // ---------- release ----------

    @Test
    void releaseByClaimerReturnsAlertToNew() {
        TriageAlert alert = inProgressAlert(9L, "DOCTOR");
        when(triageAlertRepository.releaseIfClaimedBy(eq(10L), eq(9L), eq("DOCTOR"), any()))
                .thenAnswer(invocation -> {
                    alert.setStatus(TriageAlertStatus.NEW);
                    alert.setClaimedById(null);
                    alert.setClaimedByType(null);
                    return 1;
                });

        TriageAlertResponse response = service.release(10L, 9L, "DOCTOR");

        assertEquals("NEW", response.status());
        verify(actionRepository).save(argThat(action ->
                action.getActionType() == TriageAlertActionType.RELEASED && action.getDetail() == null));
        verify(eventPublisher).publishEvent(argThat((TriageAlertStatusChangedEvent event) ->
                event.status().equals("NEW")));
        verify(triageAlertRepository, never()).releaseAsAdmin(any(), any());
    }

    @Test
    void releaseByAdminOnBehalfUsesAdminQueryAndNotesOriginalClaimer() {
        TriageAlert alert = inProgressAlert(9L, "DOCTOR");
        when(triageAlertRepository.releaseAsAdmin(eq(10L), any())).thenAnswer(invocation -> {
            alert.setStatus(TriageAlertStatus.NEW);
            return 1;
        });

        service.release(10L, 1L, "ADMIN");

        verify(actionRepository).save(argThat(action ->
                action.getActionType() == TriageAlertActionType.RELEASED
                        && action.getDetail().contains("DOCTOR #9")));
    }

    @Test
    void releaseThrowsWhenAnotherDoctorTries() {
        inProgressAlert(9L, "DOCTOR");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.release(10L, 5L, "DOCTOR"));

        assertEquals("Chỉ Bác sĩ #9 (người đã tiếp nhận) mới được trả lại cảnh báo này",
                exception.getMessage());
        verify(triageAlertRepository, never()).releaseIfClaimedBy(any(), any(), any(), any());
    }

    @Test
    void releaseThrowsWhenAlertNotClaimed() {
        TriageAlert alert = inProgressAlert(null, null);
        alert.setStatus(TriageAlertStatus.NEW);

        assertThrows(IllegalStateException.class, () -> service.release(10L, 5L, "DOCTOR"));
    }

    // ---------- resolve ----------

    @Test
    void resolveThrowsWhenNoStaffMessageSentYet() {
        inProgressAlert(7L, "DOCTOR");
        when(messageRepository.countStaffMessages(10L)).thenReturn(0L);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.resolve(10L, 7L, "DOCTOR", null));

        assertEquals("Cần gửi ít nhất 1 tin nhắn cho bệnh nhân trước khi đánh dấu xử lý xong",
                exception.getMessage());
        verify(triageAlertRepository, never()).resolveIfClaimedBy(any(), any(), any(), any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any(TriageAlertStatusChangedEvent.class));
    }

    @Test
    void resolveWithNoteMarksHandledAndBroadcastsSameTimestampUsedInDb() {
        TriageAlert alert = inProgressAlert(7L, "ADMIN");
        when(messageRepository.countStaffMessages(10L)).thenReturn(1L);
        when(triageAlertRepository.resolveIfClaimedBy(eq(10L), eq(7L), eq("ADMIN"),
                eq(TriageAlertOutcome.HANDLED), eq("Da huong dan di vien"), any()))
                .thenAnswer(invocation -> {
                    alert.setStatus(TriageAlertStatus.RESOLVED);
                    alert.setOutcome(TriageAlertOutcome.HANDLED);
                    alert.setResolutionNote("Da huong dan di vien");
                    alert.setResolvedAt(invocation.getArgument(5));
                    alert.setResolvedById(7L);
                    alert.setResolvedByType("ADMIN");
                    return 1;
                });

        TriageAlertResponse response = service.resolve(10L, 7L, "ADMIN", "  Da huong dan di vien  ");

        assertEquals("RESOLVED", response.status());
        assertEquals("HANDLED", response.outcome());
        assertEquals("Da huong dan di vien", response.resolutionNote());
        verify(actionRepository).save(argThat(action ->
                action.getActionType() == TriageAlertActionType.RESOLVED
                        && "Da huong dan di vien".equals(action.getDetail())));
        verify(eventPublisher).publishEvent(argThat((TriageAlertStatusChangedEvent event) ->
                event.status().equals("RESOLVED")
                        && event.resolvedById().equals(7L)
                        && event.statusChangedAt().equals(alert.getResolvedAt())));
        verify(triageAlertRepository, never()).resolveAsAdminOverride(any(), any(), any(), any(), any());
    }

    @Test
    void resolveThrowsWhenAlertAlreadyResolved() {
        TriageAlert alert = inProgressAlert(7L, "ADMIN");
        alert.setStatus(TriageAlertStatus.RESOLVED);

        assertThrows(IllegalStateException.class, () -> service.resolve(10L, 7L, "ADMIN", null));

        verify(eventPublisher, never()).publishEvent(any(TriageAlertStatusChangedEvent.class));
    }

    @Test
    void resolveThrowsWhenAlertStillNewAndNotYetClaimed() {
        TriageAlert alert = inProgressAlert(null, null);
        alert.setStatus(TriageAlertStatus.NEW);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.resolve(10L, 7L, "ADMIN", null));

        assertEquals("Cảnh báo id=10 phải được tiếp nhận trước khi đánh dấu xử lý xong",
                exception.getMessage());
    }

    @Test
    void resolveThrowsWhenAnotherDoctorWhoDidNotClaimTriesToResolve() {
        inProgressAlert(9L, "DOCTOR");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.resolve(10L, 5L, "DOCTOR", null));

        assertEquals("Chỉ Bác sĩ #9 (người đã tiếp nhận) mới được đánh dấu xử lý xong cảnh báo này",
                exception.getMessage());
        verify(triageAlertRepository, never()).resolveAsAdminOverride(any(), any(), any(), any(), any());
    }

    @Test
    void resolveAsAdminOverrideKeepsOriginalClaimerButMarksResolvedByAdmin() {
        TriageAlert alert = inProgressAlert(9L, "DOCTOR");
        when(messageRepository.countStaffMessages(10L)).thenReturn(1L);
        when(triageAlertRepository.resolveAsAdminOverride(eq(10L), eq(1L), eq(TriageAlertOutcome.HANDLED), any(), any()))
                .thenAnswer(invocation -> {
                    alert.setStatus(TriageAlertStatus.RESOLVED);
                    alert.setResolvedAt(invocation.getArgument(4));
                    alert.setResolvedById(1L);
                    alert.setResolvedByType("ADMIN");
                    return 1;
                });

        TriageAlertResponse response = service.resolve(10L, 1L, "ADMIN", null);

        assertEquals("RESOLVED", response.status());
        assertEquals(1L, response.resolvedById());
        assertEquals(9L, response.claimedById());
        assertEquals("DOCTOR", response.claimedByType());
        verify(eventPublisher).publishEvent(argThat((TriageAlertStatusChangedEvent event) ->
                event.resolvedByType().equals("ADMIN") && event.claimedById().equals(9L)));
    }

    // ---------- false alarm ----------

    @Test
    void markFalseAlarmRequiresReason() {
        inProgressAlert(7L, "DOCTOR");

        assertThrows(IllegalArgumentException.class, () -> service.markFalseAlarm(10L, 7L, "DOCTOR", null));
        assertThrows(IllegalArgumentException.class, () -> service.markFalseAlarm(10L, 7L, "DOCTOR", "   "));
        assertThrows(IllegalArgumentException.class, () -> service.markFalseAlarm(10L, 7L, "DOCTOR", " ab "));

        verify(triageAlertRepository, never()).resolveIfClaimedBy(any(), any(), any(), any(), any(), any());
    }

    @Test
    void markFalseAlarmClosesWithoutRequiringStaffMessage() {
        TriageAlert alert = inProgressAlert(7L, "DOCTOR");
        when(triageAlertRepository.resolveIfClaimedBy(eq(10L), eq(7L), eq("DOCTOR"),
                eq(TriageAlertOutcome.FALSE_ALARM), eq("Benh nhan thu he thong"), any()))
                .thenAnswer(invocation -> {
                    alert.setStatus(TriageAlertStatus.RESOLVED);
                    alert.setOutcome(TriageAlertOutcome.FALSE_ALARM);
                    return 1;
                });

        TriageAlertResponse response = service.markFalseAlarm(10L, 7L, "DOCTOR", "Benh nhan thu he thong");

        assertEquals("FALSE_ALARM", response.outcome());
        verify(actionRepository).save(argThat(action ->
                action.getActionType() == TriageAlertActionType.FALSE_ALARM
                        && "Benh nhan thu he thong".equals(action.getDetail())));
        verify(eventPublisher).publishEvent(argThat((TriageAlertStatusChangedEvent event) ->
                event.status().equals("RESOLVED")));
    }

    // ---------- list ----------

    @Test
    void listOpenQueriesNewAndInProgressAndFillsCounters() {
        Patient patient = patient(1L, "Nguyen Van A", "0901234567");
        TriageAlert alert = new TriageAlert(patient, 2L, 3L, "Cau hoi khan cap",
                "[\"XUAT_HUYET_TIEU_HOA\",\"DAU_BUNG_CAP_TINH\"]",
                Instant.parse("2026-09-30T00:00:00Z"));
        alert.setId(10L);
        Page<TriageAlert> page = new PageImpl<>(List.of(alert));
        when(triageAlertRepository.findPageByStatuses(
                eq(Set.of(TriageAlertStatus.NEW, TriageAlertStatus.IN_PROGRESS)), any()))
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
    void getDetailMarksPatientMessagesReadBeforeLoading() {
        inProgressAlert(7L, "DOCTOR");

        TriageAlertDetailResponse detail = service.getDetail(10L);

        assertEquals(10L, detail.alert().id());
        verify(messageRepository).markPatientMessagesRead(eq(10L), any());
    }

    @Test
    void getDetailThrowsNotFoundForUnknownAlert() {
        when(triageAlertRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(vn.gastroai.be.application.support.ResourceNotFoundException.class,
                () -> service.getDetail(404L));
    }
}
