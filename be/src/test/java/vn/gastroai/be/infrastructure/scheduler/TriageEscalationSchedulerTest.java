package vn.gastroai.be.infrastructure.scheduler;

import org.junit.jupiter.api.Test;
import vn.gastroai.be.application.audit.AuditLogService;
import vn.gastroai.be.application.triage.TriageAlertResponse;
import vn.gastroai.be.application.triage.TriageAlertService;
import vn.gastroai.be.domain.audit.AuditAction;
import vn.gastroai.be.infrastructure.persistence.postgres.TriageAlertRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TriageEscalationSchedulerTest {

    private static final Instant NOW = Instant.parse("2026-11-20T10:00:00Z");

    private final TriageAlertRepository repository = mock(TriageAlertRepository.class);
    private final TriageAlertService service = mock(TriageAlertService.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);

    private final TriageEscalationScheduler scheduler =
            new TriageEscalationScheduler(repository, service, auditLogService, 3, 5);

    private static TriageAlertResponse alert(Long id, Long patientId) {
        return new TriageAlertResponse(id, patientId, "Nguyen Van A", null, null, null, "Non ra mau",
                List.of(), "NEEDS_INTERVENTION", null, null, null, null, null, null, NOW,
                null, null, 0, null, null, 0, 0, 0, NOW, null, null, null, null);
    }

    @Test
    void escalatesAlertsSilentLongerThanThreeMinutesAndAudits() {
        when(repository.findIdsDueForEscalation(NOW.minus(Duration.ofMinutes(3)))).thenReturn(List.of(10L));
        when(service.escalateIfStillSilent(10L, Duration.ofMinutes(3))).thenReturn(Optional.of(alert(10L, 1L)));

        scheduler.escalateSilentAlerts(NOW);

        verify(auditLogService).record(AuditAction.TRIAGE_ALERT_ESCALATE, "TRIAGE_ALERT", 10L,
                "Bệnh nhân #1 · không phản hồi sau 3 phút → Cần can thiệp");
    }

    @Test
    void doesNotAuditWhenPatientAnsweredJustBefore() {
        when(repository.findIdsDueForEscalation(any())).thenReturn(List.of(10L));
        when(service.escalateIfStillSilent(10L, Duration.ofMinutes(3))).thenReturn(Optional.empty());

        scheduler.escalateSilentAlerts(NOW);

        verify(auditLogService, never()).record(any(), any(), any(), any());
    }

    @Test
    void runsFailsafeAfterFiveMinutesWithoutAdmin() {
        when(repository.findIdsDueForFailsafe(NOW.minus(Duration.ofMinutes(5)))).thenReturn(List.of(10L));
        when(service.runFailsafeIfUnacknowledged(10L, Duration.ofMinutes(5))).thenReturn(Optional.of(alert(10L, 1L)));

        scheduler.runFailsafe(NOW);

        verify(auditLogService).record(AuditAction.TRIAGE_ALERT_FAILSAFE, "TRIAGE_ALERT", 10L,
                "Bệnh nhân #1 · tự động do không có Admin xác nhận trong 5 phút");
    }

    @Test
    void oneFailingAlertDoesNotBlockTheOthers() {
        when(repository.findIdsDueForEscalation(any())).thenReturn(List.of(10L, 11L));
        when(service.escalateIfStillSilent(10L, Duration.ofMinutes(3))).thenThrow(new IllegalStateException("db"));
        when(service.escalateIfStillSilent(11L, Duration.ofMinutes(3))).thenReturn(Optional.of(alert(11L, 2L)));

        scheduler.escalateSilentAlerts(NOW);

        verify(auditLogService).record(AuditAction.TRIAGE_ALERT_ESCALATE, "TRIAGE_ALERT", 11L,
                "Bệnh nhân #2 · không phản hồi sau 3 phút → Cần can thiệp");
    }

    @Test
    void minutesBelowOneAreRaisedToOne() {
        TriageEscalationScheduler fast = new TriageEscalationScheduler(repository, service, auditLogService, 0, 0);
        when(repository.findIdsDueForEscalation(any())).thenReturn(List.of());

        fast.escalateSilentAlerts(NOW);

        verify(repository).findIdsDueForEscalation(NOW.minus(Duration.ofMinutes(1)));
    }
}
