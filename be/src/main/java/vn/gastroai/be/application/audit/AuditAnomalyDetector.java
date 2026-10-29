package vn.gastroai.be.application.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.gastroai.be.api.notification.NotificationResponse;
import vn.gastroai.be.application.notification.AppNotificationService;
import vn.gastroai.be.domain.audit.AuditAction;
import vn.gastroai.be.domain.audit.AuditAnomalyRule;
import vn.gastroai.be.domain.audit.AuditLog;
import vn.gastroai.be.infrastructure.persistence.mysql.AuditAnomalyRuleRepository;
import vn.gastroai.be.infrastructure.persistence.mysql.AuditLogRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/** UC0063 - phat hien truy cap bat thuong ngay sau khi ghi 1 audit log. */
@Component
public class AuditAnomalyDetector {

    private static final Logger log = LoggerFactory.getLogger(AuditAnomalyDetector.class);
    private static final String RESTORE_CODE = "SYSTEM_RESTORE";

    private final AuditLogRepository auditLogRepository;
    private final AuditAnomalyRuleRepository ruleRepository;
    private final AppNotificationService notificationService;

    public AuditAnomalyDetector(AuditLogRepository auditLogRepository,
                                AuditAnomalyRuleRepository ruleRepository,
                                AppNotificationService notificationService) {
        this.auditLogRepository = auditLogRepository;
        this.ruleRepository = ruleRepository;
        this.notificationService = notificationService;
    }

    public void check(AuditLog saved) {
        try {
            AuditAction action = saved.getAction();
            if (action == AuditAction.ANOMALY_DETECTED || action == AuditAction.ANOMALY_RULE_UPDATE) {
                return;
            }
            if (action == AuditAction.SYSTEM_RESTORE) {
                alertRestore(saved);
                return;
            }
            if (saved.getActorId() == null) {
                return;
            }

            Instant now = Instant.now();
            for (AuditAnomalyRule rule : ruleRepository.findByEnabledTrue()) {
                if (!rule.appliesTo(action)) {
                    continue;
                }
                Instant since = now.minus(Duration.ofMinutes(rule.getWindowMinutes()));
                long count = count(rule, saved, since);
                if (count >= rule.getThreshold()) {
                    alert(rule, saved, count, now);
                }
            }
        } catch (RuntimeException exception) {
            log.error("Kiem tra bat thuong that bai cho audit log id={}", saved.getId(), exception);
        }
    }

    private long count(AuditAnomalyRule rule, AuditLog saved, Instant since) {
        return switch (rule.getMetric()) {
            case ACTIONS -> auditLogRepository.countActions(
                    saved.getActorType(), saved.getActorId(), rule.getActionList(), since);
            case DISTINCT_TARGETS -> auditLogRepository.countDistinctTargets(
                    saved.getActorType(), saved.getActorId(), rule.getActionList(), since);
            case DISTINCT_IPS -> auditLogRepository.countDistinctIps(
                    saved.getActorType(), saved.getActorId(), since);
        };
    }

    private void alert(AuditAnomalyRule rule, AuditLog saved, long count, Instant now) {
        long windowSeconds = rule.getWindowMinutes() * 60L;
        long bucket = now.getEpochSecond() / windowSeconds;
        String dedupeKey = "AUDIT_ANOMALY:" + rule.getCode() + ":"
                + saved.getActorType() + ":" + saved.getActorId() + ":" + bucket;

        String detail = "%s: %d trong %d phút (ngưỡng %d)".formatted(
                rule.getName(), count, rule.getWindowMinutes(), rule.getThreshold());

        Optional<NotificationResponse> created = notificationService.createAuditAnomalyAlert(
                "Cảnh báo bất thường: " + rule.getName(),
                actorText(saved) + " - " + detail,
                dedupeKey);

        if (created.isPresent()) {
            saveAnomalyLog(saved, rule.getCode(), detail);
        }
    }

    private void alertRestore(AuditLog saved) {
        String detail = "Dữ liệu hệ thống vừa được khôi phục (" + saved.getDetail() + ")";
        Optional<NotificationResponse> created = notificationService.createAuditAnomalyAlert(
                "Cảnh báo: dữ liệu vừa được khôi phục",
                actorText(saved) + " - " + detail,
                "AUDIT_ANOMALY:" + RESTORE_CODE + ":" + saved.getId());

        if (created.isPresent()) {
            saveAnomalyLog(saved, RESTORE_CODE, detail);
        }
    }

    private void saveAnomalyLog(AuditLog saved, String ruleCode, String detail) {
        auditLogRepository.save(new AuditLog(
                Instant.now(),
                saved.getActorType(),
                saved.getActorId(),
                saved.getActorEmail(),
                AuditAction.ANOMALY_DETECTED,
                "RULE",
                ruleCode,
                detail,
                saved.getIpAddress(),
                saved.getUserAgent()));
    }

    private String actorText(AuditLog saved) {
        if (saved.getActorEmail() != null) {
            return saved.getActorEmail();
        }
        if (saved.getActorId() != null) {
            return saved.getActorType() + " #" + saved.getActorId();
        }
        return "Hệ thống";
    }
}