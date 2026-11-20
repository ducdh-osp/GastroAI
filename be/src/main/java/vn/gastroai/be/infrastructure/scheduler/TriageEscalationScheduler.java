package vn.gastroai.be.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.gastroai.be.application.audit.AuditLogService;
import vn.gastroai.be.application.triage.TriageAlertService;
import vn.gastroai.be.domain.audit.AuditAction;
import vn.gastroai.be.infrastructure.persistence.postgres.TriageAlertRepository;

import java.time.Duration;
import java.time.Instant;

/**
 * SCR-AD-13 - bo hen gio cua luong canh bao khan cap, chay doc lap voi viec Admin co mo man hay khong.
 * <ul>
 *   <li>Buoc 3: ca "dang tu xu ly" qua {@code interventionAfter} ma benh nhan im lang -> "Can can thiep".
 *       Mac dinh 3 phut: khong co SMS, khong co goi tu dong nen rut ngan nhu BA goi y.</li>
 *   <li>Buoc 4 (failsafe): ca "can can thiep" qua {@code failsafeAfter} ma chua Admin nao ra tay
 *       -> bao dong lai moi Admin, nhac lai benh nhan.</li>
 * </ul>
 * Moi ca xu ly trong transaction rieng (qua TriageAlertService) - 1 ca loi khong chan cac ca khac.
 * Dieu kien trong cau UPDATE chan viec 2 noi cung chuyen 1 ca (vd benh nhan vua bam nut).
 */
@Component
public class TriageEscalationScheduler {

    private static final Logger log = LoggerFactory.getLogger(TriageEscalationScheduler.class);
    private static final String TARGET = "TRIAGE_ALERT";

    private final TriageAlertRepository triageAlertRepository;
    private final TriageAlertService triageAlertService;
    private final AuditLogService auditLogService;
    private final Duration interventionAfter;
    private final Duration failsafeAfter;

    public TriageEscalationScheduler(
            TriageAlertRepository triageAlertRepository,
            TriageAlertService triageAlertService,
            AuditLogService auditLogService,
            @Value("${app.triage.intervention-after-minutes:3}") long interventionAfterMinutes,
            @Value("${app.triage.failsafe-after-minutes:5}") long failsafeAfterMinutes) {
        this.triageAlertRepository = triageAlertRepository;
        this.triageAlertService = triageAlertService;
        this.auditLogService = auditLogService;
        this.interventionAfter = Duration.ofMinutes(Math.max(1, interventionAfterMinutes));
        this.failsafeAfter = Duration.ofMinutes(Math.max(1, failsafeAfterMinutes));
    }

    @Scheduled(fixedDelayString = "${app.triage.timer-delay-ms:20000}")
    public void run() {
        Instant now = Instant.now();
        escalateSilentAlerts(now);
        runFailsafe(now);
    }

    void escalateSilentAlerts(Instant now) {
        for (Long alertId : triageAlertRepository.findIdsDueForEscalation(now.minus(interventionAfter))) {
            try {
                triageAlertService.escalateIfStillSilent(alertId, interventionAfter).ifPresent(alert ->
                        auditLogService.record(AuditAction.TRIAGE_ALERT_ESCALATE, TARGET, alertId,
                                "Bệnh nhân #" + alert.patientId() + " · không phản hồi sau "
                                        + interventionAfter.toMinutes() + " phút → Cần can thiệp"));
            } catch (RuntimeException exception) {
                log.error("Khong chuyen duoc canh bao Triage id={} sang Can can thiep: {}",
                        alertId, exception.getMessage(), exception);
            }
        }
    }

    void runFailsafe(Instant now) {
        for (Long alertId : triageAlertRepository.findIdsDueForFailsafe(now.minus(failsafeAfter))) {
            try {
                triageAlertService.runFailsafeIfUnacknowledged(alertId, failsafeAfter).ifPresent(alert ->
                        auditLogService.record(AuditAction.TRIAGE_ALERT_FAILSAFE, TARGET, alertId,
                                "Bệnh nhân #" + alert.patientId() + " · tự động do không có Admin xác nhận trong "
                                        + failsafeAfter.toMinutes() + " phút"));
            } catch (RuntimeException exception) {
                log.error("Failsafe canh bao Triage id={} loi: {}", alertId, exception.getMessage(), exception);
            }
        }
    }
}
