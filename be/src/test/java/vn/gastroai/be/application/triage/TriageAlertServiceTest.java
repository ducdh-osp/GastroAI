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

        // UC0067(va) - tra ve dung id cua canh bao vua tao, de ChatController dung lai goi
        // linkConversation() sau khi saveExchange() xong.
        assertEquals(100L, alertId);

        // Phai luu vao DB voi status NEW, dung noi dung/nhom trieu chung.
        verify(triageAlertRepository).save(argThat(alert ->
                alert.getPatient().equals(patient)
                        && alert.getStatus() == TriageAlertStatus.NEW
                        && alert.getMessageContent().equals("Toi bi dau bung du doi va non ra mau")
                        && alert.getSessionId() == null
                        && alert.getMessageId() == null));

        // Phai phat su kien da lam giau ten/SDT benh nhan + id vua luu DB.
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

        // UC0067(va) - phai tra ve id CUA CANH BAO CU (50), de linkConversation() sau nay
        // gan dung vao canh bao dang hien thi tren man hinh admin, khong tao canh bao moi.
        assertEquals(50L, alertId);

        // Phai cap nhat CHINH canh bao cu (id=50) voi noi dung moi, khong tao ban ghi moi.
        verify(triageAlertRepository).save(argThat(alert ->
                alert.getId().equals(50L)
                        && alert.getMessageContent().equals("Toi bi dau bung du doi hon nhieu roi")));

        // Event phat ra phai mang DUNG id cua canh bao cu, de FE cap nhat tai cho thay vi
        // hien them 1 dong trung lap.
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

        // UC0067(va) - sau linkConversation(), canh bao phai mang dung sessionId/messageId
        // that, va phai gui lai event qua WebSocket de FE upsert dong da co san.
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

        // Khong duoc nem ngoai le - canh bao co the da bi xoa/khong con, chi bo qua.
        service.linkConversation(999L, 7L, 9L);

        verify(triageAlertRepository, never()).save(any());
        verify(triageAlertPublisher, never()).publish(any());
    }

    @Test
    void claimTransitionsToInProgressAndBroadcastsStatusChange() {
        Patient patient = patient(1L, "Nguyen Van A", "0901234567");
        TriageAlert alert = new TriageAlert(patient, null, null, "Cau hoi khan cap",
                "[\"DAU_BUNG_CAP_TINH\"]", Instant.parse("2026-09-30T00:00:00Z"));
        alert.setId(10L);
        when(triageAlertRepository.findById(10L)).thenReturn(Optional.of(alert));
        when(triageAlertRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TriageAlertResponse response = service.claim(10L, 5L, "ADMIN");

        assertEquals("IN_PROGRESS", response.status());
        assertEquals(5L, response.claimedById());
        assertEquals("ADMIN", response.claimedByType());
        assertEquals(List.of("DAU_BUNG_CAP_TINH"), response.matchedGroups());

        verify(triageAlertPublisher).publishStatusChange(argThat((TriageAlertStatusChangedEvent event) ->
                event.id().equals(10L)
                        && event.status().equals("IN_PROGRESS")
                        && event.claimedById().equals(5L)
                        && event.claimedByType().equals("ADMIN")));
    }

    @Test
    void claimThrowsWhenAlertAlreadyResolved() {
        Patient patient = patient(1L, "Nguyen Van A", "0901234567");
        TriageAlert alert = new TriageAlert(patient, null, null, "Cau hoi khan cap",
                null, Instant.parse("2026-09-30T00:00:00Z"));
        alert.setId(10L);
        alert.setStatus(TriageAlertStatus.RESOLVED);
        when(triageAlertRepository.findById(10L)).thenReturn(Optional.of(alert));

        assertThrows(IllegalStateException.class, () -> service.claim(10L, 5L, "ADMIN"));

        verify(triageAlertRepository, never()).save(any());
        verify(triageAlertPublisher, never()).publishStatusChange(any());
    }

    @Test
    void resolveTransitionsToResolvedAndBroadcastsStatusChange() {
        Patient patient = patient(1L, "Nguyen Van A", "0901234567");
        TriageAlert alert = new TriageAlert(patient, null, null, "Cau hoi khan cap",
                null, Instant.parse("2026-09-30T00:00:00Z"));
        alert.setId(10L);
        alert.setStatus(TriageAlertStatus.IN_PROGRESS);
        alert.setClaimedById(5L);
        alert.setClaimedByType("ADMIN");
        when(triageAlertRepository.findById(10L)).thenReturn(Optional.of(alert));
        when(triageAlertRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TriageAlertResponse response = service.resolve(10L);

        assertEquals("RESOLVED", response.status());
        verify(triageAlertPublisher).publishStatusChange(argThat((TriageAlertStatusChangedEvent event) ->
                event.id().equals(10L) && event.status().equals("RESOLVED")));
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