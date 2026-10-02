package vn.gastroai.be.application.triage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.triage.TriageAlert;
import vn.gastroai.be.domain.triage.TriageAlertEvent;
import vn.gastroai.be.domain.triage.TriageAlertStatus;
import vn.gastroai.be.domain.triage.TriageAlertStatusChangedEvent;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.TriageAlertRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

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
    private final PatientRepository patientRepository = mock(PatientRepository.class);
    private final TriageAlertPublisher triageAlertPublisher = mock(TriageAlertPublisher.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final TriageAlertService service = new TriageAlertService(
            triageAlertRepository, patientRepository, triageAlertPublisher, objectMapper);

    private Patient patient(Long id, String fullName, String phone) {
        Patient patient = new Patient();
        patient.setId(id);
        patient.setFullName(fullName);
        patient.setPhone(phone);
        return patient;
    }

    @Test
    void createAndPublishSavesAlertAndPublishesEnrichedEvent() {
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
                        && alert.getSessionId() == null
                        && alert.getMessageId() == null));

        verify(triageAlertPublisher).publish(argThat((TriageAlertEvent event) ->
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
        verify(triageAlertPublisher, never()).publish(any());
    }

    @Test
    void createAndPublishMergesIntoExistingUnresolvedAlertWithinDedupeWindow() {
      
        Patient patient = patient(1L, "Nguyen Van A", "0901234567");
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

        TriageAlert existingAlert = new TriageAlert(patient, null, null, "Dau bung nhe",
                "[\"DAU_BUNG_NHE\"]", Instant.now().minusSeconds(60));
        existingAlert.setId(50L);
        when(triageAlertRepository.findFirstByPatient_IdAndStatusNotAndOccurredAtAfterOrderByOccurredAtDesc(
                eq(1L), eq(TriageAlertStatus.RESOLVED), any()))
                .thenReturn(Optional.of(existingAlert));
        when(triageAlertRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Long alertId = service.createAndPublish(
                1L, null, null,
                "Toi bi dau bung du doi hon nhieu roi",
                List.of("DAU_BUNG_CAP_TINH"));

        assertEquals(50L, alertId);

        verify(triageAlertRepository).save(argThat(alert ->
                alert.getId().equals(50L)
                        && alert.getMessageContent().equals("Toi bi dau bung du doi hon nhieu roi")));

        verify(triageAlertPublisher).publish(argThat((TriageAlertEvent event) ->
                event.id().equals(50L)
                        && event.messageContent().equals("Toi bi dau bung du doi hon nhieu roi")));
    }

    @Test
    void createAndPublishCreatesNewAlertWhenNoRecentUnresolvedAlertExists() {
        Patient patient = patient(1L, "Nguyen Van A", "0901234567");
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(triageAlertRepository.findFirstByPatient_IdAndStatusNotAndOccurredAtAfterOrderByOccurredAtDesc(
                eq(1L), eq(TriageAlertStatus.RESOLVED), any()))
                .thenReturn(Optional.empty());
        when(triageAlertRepository.save(any())).thenAnswer(invocation -> {
            TriageAlert saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        service.createAndPublish(
                1L, null, null,
                "Dau bung cap tinh lan dau",
                List.of("DAU_BUNG_CAP_TINH"));

        verify(triageAlertRepository).save(argThat(alert ->
                alert.getPatient().equals(patient)
                        && alert.getMessageContent().equals("Dau bung cap tinh lan dau")));
        verify(triageAlertPublisher).publish(argThat((TriageAlertEvent event) -> event.id().equals(100L)));
    }

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
                saved.getId().equals(100L)
                        && saved.getSessionId().equals(7L)
                        && saved.getMessageId().equals(9L)));

        verify(triageAlertPublisher).publish(argThat((TriageAlertEvent event) ->
                event.id().equals(100L)
                        && event.sessionId().equals(7L)
                        && event.messageId().equals(9L)
                        && event.messageContent().equals("Toi bi dau bung du doi qua")));
    }

    @Test
    void linkConversationDoesNothingWhenAlertNoLongerExists() {
        when(triageAlertRepository.findById(999L)).thenReturn(Optional.empty());

        service.linkConversation(999L, 7L, 9L);

        verify(triageAlertRepository, never()).save(any());
        verify(triageAlertPublisher, never()).publish(any());
    }

    @Test
    void claimTransitionsToInProgressAndBroadcastsSameTimestampUsedInDb() {
        Patient patient = patient(1L, "Nguyen Van A", "0901234567");
        TriageAlert alert = new TriageAlert(patient, null, null, "Cau hoi khan cap",
                "[\"DAU_BUNG_CAP_TINH\"]", Instant.parse("2026-09-30T00:00:00Z"));
        alert.setId(10L);
        when(triageAlertRepository.findById(10L)).thenReturn(Optional.of(alert));
        // UC0067(va) - mo phong hieu ung cua UPDATE nguyen tu that trong DB: sua truc tiep
        // len CUNG 1 instance ma findById() tra ve, dung CHINH gia tri Instant "now" ma
        // service truyen vao - de kiem tra duoc lỗi 4 (statusChangedAt phai TRUNG claimedAt).
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
        assertEquals("ADMIN", response.claimedByType());
        assertEquals(List.of("DAU_BUNG_CAP_TINH"), response.matchedGroups());

        verify(triageAlertPublisher).publishStatusChange(argThat((TriageAlertStatusChangedEvent event) ->
                event.id().equals(10L)
                        && event.status().equals("IN_PROGRESS")
                        && event.claimedById().equals(5L)
                        && event.claimedByType().equals("ADMIN")
                        // UC0067(va) - lỗi 4: statusChangedAt gui cho FE phai CUNG gia tri
                        // voi claimedAt vua ghi vao DB, khong duoc lay tu updatedAt (chi
                        // duoc Hibernate cap nhat luc flush, tuc la SAU khi da broadcast).
                        && event.statusChangedAt().equals(alert.getClaimedAt())));
    }

    @Test
    void claimThrowsWhenAlertAlreadyResolved() {
        Patient patient = patient(1L, "Nguyen Van A", "0901234567");
        TriageAlert alert = new TriageAlert(patient, null, null, "Cau hoi khan cap",
                null, Instant.parse("2026-09-30T00:00:00Z"));
        alert.setId(10L);
        alert.setStatus(TriageAlertStatus.RESOLVED);
        when(triageAlertRepository.findById(10L)).thenReturn(Optional.of(alert));
        when(triageAlertRepository.claimIfNew(eq(10L), any(), any(), any())).thenReturn(0);

        assertThrows(IllegalStateException.class, () -> service.claim(10L, 5L, "ADMIN"));

        verify(triageAlertPublisher, never()).publishStatusChange(any());
    }

    @Test
    void claimReturnsCurrentStateWhenSameClaimerRepeatsRequest() {
        Patient patient = patient(1L, "Nguyen Van A", "0901234567");
        TriageAlert alert = new TriageAlert(patient, null, null, "Cau hoi khan cap",
                "[\"DAU_BUNG_CAP_TINH\"]", Instant.parse("2026-09-30T00:00:00Z"));
        alert.setId(10L);
        alert.setStatus(TriageAlertStatus.IN_PROGRESS);
        alert.setClaimedById(5L);
        alert.setClaimedByType("ADMIN");
        when(triageAlertRepository.findById(10L)).thenReturn(Optional.of(alert));
        // UC0067(va) - canh bao da la IN_PROGRESS boi CHINH nguoi nay (vd bam dup, hoac
        // mang gui lai request) nen UPDATE ... WHERE status = NEW khong con khop, tra ve 0 -
        // nhung day KHONG phai loi, phai tra ve binh thuong.
        when(triageAlertRepository.claimIfNew(eq(10L), eq(5L), eq("ADMIN"), any())).thenReturn(0);

        TriageAlertResponse response = service.claim(10L, 5L, "ADMIN");

        assertEquals("IN_PROGRESS", response.status());
        assertEquals(5L, response.claimedById());
        verify(triageAlertPublisher, never()).publishStatusChange(any());
    }

    @Test
    void claimThrowsWhenAnotherStaffAlreadyClaimedConcurrently() {
        Patient patient = patient(1L, "Nguyen Van A", "0901234567");
        TriageAlert alert = new TriageAlert(patient, null, null, "Cau hoi khan cap",
                null, Instant.parse("2026-09-30T00:00:00Z"));
        alert.setId(10L);
        alert.setStatus(TriageAlertStatus.IN_PROGRESS);
        alert.setClaimedById(9L);
        alert.setClaimedByType("DOCTOR");
        when(triageAlertRepository.findById(10L)).thenReturn(Optional.of(alert));
        when(triageAlertRepository.claimIfNew(eq(10L), eq(5L), eq("ADMIN"), any())).thenReturn(0);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.claim(10L, 5L, "ADMIN"));

        // UC0067(va) - thong bao phai neu ro ai da tiep nhan, giup admin khac biet lien he
        // ai thay vi chi bao chung chung "da co nguoi tiep nhan".
        assertEquals("Cảnh báo đã được Bác sĩ #9 tiếp nhận", exception.getMessage());
        verify(triageAlertPublisher, never()).publishStatusChange(any());
    }

    @Test
    void resolveTransitionsToResolvedAndBroadcastsSameTimestampUsedInDb() {
        Patient patient = patient(1L, "Nguyen Van A", "0901234567");
        TriageAlert alert = new TriageAlert(patient, null, null, "Cau hoi khan cap",
                null, Instant.parse("2026-09-30T00:00:00Z"));
        alert.setId(10L);
        alert.setStatus(TriageAlertStatus.IN_PROGRESS);
        alert.setClaimedById(5L);
        alert.setClaimedByType("ADMIN");
        when(triageAlertRepository.findById(10L)).thenReturn(Optional.of(alert));
        when(triageAlertRepository.resolveIfInProgress(eq(10L), any())).thenAnswer(invocation -> {
            Instant now = invocation.getArgument(1);
            alert.setStatus(TriageAlertStatus.RESOLVED);
            alert.setResolvedAt(now);
            return 1;
        });

        TriageAlertResponse response = service.resolve(10L);

        assertEquals("RESOLVED", response.status());
        verify(triageAlertPublisher).publishStatusChange(argThat((TriageAlertStatusChangedEvent event) ->
                event.id().equals(10L)
                        && event.status().equals("RESOLVED")
                        && event.statusChangedAt().equals(alert.getResolvedAt())));
    }

    @Test
    void resolveThrowsWhenAlertAlreadyResolved() {
        Patient patient = patient(1L, "Nguyen Van A", "0901234567");
        TriageAlert alert = new TriageAlert(patient, null, null, "Cau hoi khan cap",
                null, Instant.parse("2026-09-30T00:00:00Z"));
        alert.setId(10L);
        alert.setStatus(TriageAlertStatus.RESOLVED);
        when(triageAlertRepository.findById(10L)).thenReturn(Optional.of(alert));
        when(triageAlertRepository.resolveIfInProgress(eq(10L), any())).thenReturn(0);

        assertThrows(IllegalStateException.class, () -> service.resolve(10L));

        verify(triageAlertPublisher, never()).publishStatusChange(any());
    }

    @Test
    void resolveThrowsWhenAlertStillNewAndNotYetClaimed() {
        Patient patient = patient(1L, "Nguyen Van A", "0901234567");
        TriageAlert alert = new TriageAlert(patient, null, null, "Cau hoi khan cap",
                null, Instant.parse("2026-09-30T00:00:00Z"));
        alert.setId(10L);
        when(triageAlertRepository.findById(10L)).thenReturn(Optional.of(alert));
        when(triageAlertRepository.resolveIfInProgress(eq(10L), any())).thenReturn(0);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.resolve(10L));

        assertEquals("Cảnh báo id=10 phải được tiếp nhận trước khi đánh dấu đã xử lý",
        exception.getMessage());
        verify(triageAlertPublisher, never()).publishStatusChange(any());
    }

    @Test
    void listRecentMapsEntitiesToResponsesWithParsedMatchedGroups() {
        Patient patient = patient(1L, "Nguyen Van A", "0901234567");
        TriageAlert alert = new TriageAlert(patient, 2L, 3L, "Cau hoi khan cap",
                "[\"XUAT_HUYET_TIEU_HOA\",\"DAU_BUNG_CAP_TINH\"]",
                Instant.parse("2026-09-30T00:00:00Z"));
        alert.setId(10L);
        when(triageAlertRepository.findTop50ByOrderByOccurredAtDesc()).thenReturn(List.of(alert));

        List<TriageAlertResponse> responses = service.listRecent();

        assertEquals(1, responses.size());
        TriageAlertResponse response = responses.get(0);
        assertEquals(10L, response.id());
        assertEquals(1L, response.patientId());
        assertEquals("Nguyen Van A", response.patientFullName());
        assertEquals(List.of("XUAT_HUYET_TIEU_HOA", "DAU_BUNG_CAP_TINH"), response.matchedGroups());
    }
}