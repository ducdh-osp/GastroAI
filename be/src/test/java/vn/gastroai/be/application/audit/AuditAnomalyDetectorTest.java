package vn.gastroai.be.application.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import vn.gastroai.be.api.notification.NotificationResponse;
import vn.gastroai.be.application.notification.AppNotificationService;
import vn.gastroai.be.domain.audit.AuditAction;
import vn.gastroai.be.domain.audit.AuditAnomalyRule;
import vn.gastroai.be.domain.audit.AuditLog;
import vn.gastroai.be.infrastructure.persistence.mysql.AuditAnomalyRuleRepository;
import vn.gastroai.be.infrastructure.persistence.mysql.AuditLogRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class AuditAnomalyDetectorTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private AuditAnomalyRuleRepository ruleRepository;
    @Mock private AppNotificationService notificationService;

    private AuditAnomalyDetector detector;

    private static final List<AuditAction> LOCK_ACTIONS =
            List.of(AuditAction.PATIENT_LOCK, AuditAction.PATIENT_UNLOCK);

    @BeforeEach
    void setUp() {
        detector = new AuditAnomalyDetector(auditLogRepository, ruleRepository, notificationService);
    }

    private AuditLog log(AuditAction action, Long actorId) {
        AuditLog log = new AuditLog(Instant.now(), "ADMIN", actorId, "admin@gastroai.vn", action,
                "PATIENT", "7", null, "10.0.0.1", "JUnit");
        ReflectionTestUtils.setField(log, "id", 99L);
        return log;
    }

    private AuditAnomalyRule lockRule() {
        AuditAnomalyRule rule = mock(AuditAnomalyRule.class, withSettings().strictness(Strictness.LENIENT));
        when(rule.getCode()).thenReturn("MASS_PATIENT_LOCK");
        when(rule.getName()).thenReturn("Khoá/mở khoá nhiều bệnh nhân");
        when(rule.getMetric()).thenReturn(AuditAnomalyRule.Metric.DISTINCT_TARGETS);
        when(rule.getActionList()).thenReturn(LOCK_ACTIONS);
        when(rule.getThreshold()).thenReturn(10);
        when(rule.getWindowMinutes()).thenReturn(10);
        when(rule.appliesTo(any())).thenAnswer(call -> LOCK_ACTIONS.contains(call.getArgument(0)));
        return rule;
    }

    @Test
    void reachingThresholdSendsAlertAndWritesAnomalyLog() {
        AuditAnomalyRule rule = lockRule();
        when(ruleRepository.findByEnabledTrue()).thenReturn(List.of(rule));
        when(auditLogRepository.countDistinctTargets(eq("ADMIN"), eq(1L), eq(LOCK_ACTIONS), any()))
                .thenReturn(10L);
        when(notificationService.createAuditAnomalyAlert(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(mock(NotificationResponse.class)));

        detector.check(log(AuditAction.PATIENT_LOCK, 1L));

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> dedupeKey = ArgumentCaptor.forClass(String.class);
        verify(notificationService).createAuditAnomalyAlert(
                eq("Cảnh báo bất thường: Khoá/mở khoá nhiều bệnh nhân"), message.capture(), dedupeKey.capture());
        assertEquals("admin@gastroai.vn - Khoá/mở khoá nhiều bệnh nhân: 10 trong 10 phút (ngưỡng 10)",
                message.getValue());
        assertTrue(dedupeKey.getValue().startsWith("AUDIT_ANOMALY:MASS_PATIENT_LOCK:ADMIN:1:"));

        ArgumentCaptor<AuditLog> saved = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(saved.capture());
        assertEquals(AuditAction.ANOMALY_DETECTED, saved.getValue().getAction());
        assertEquals("RULE", saved.getValue().getTargetType());
        assertEquals("MASS_PATIENT_LOCK", saved.getValue().getTargetId());
        assertEquals("admin@gastroai.vn", saved.getValue().getActorEmail());
        assertEquals("10.0.0.1", saved.getValue().getIpAddress());
    }

    @Test
    void belowThresholdDoesNothing() {
        AuditAnomalyRule rule = lockRule();
        when(ruleRepository.findByEnabledTrue()).thenReturn(List.of(rule));
        when(auditLogRepository.countDistinctTargets(eq("ADMIN"), eq(1L), eq(LOCK_ACTIONS), any()))
                .thenReturn(9L);

        detector.check(log(AuditAction.PATIENT_LOCK, 1L));

        verify(notificationService, never()).createAuditAnomalyAlert(any(), any(), any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void secondAlertInSameWindowDoesNotWriteAnotherLog() {
        AuditAnomalyRule rule = lockRule();
        when(ruleRepository.findByEnabledTrue()).thenReturn(List.of(rule));
        when(auditLogRepository.countDistinctTargets(eq("ADMIN"), eq(1L), eq(LOCK_ACTIONS), any()))
                .thenReturn(12L);
        when(notificationService.createAuditAnomalyAlert(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        detector.check(log(AuditAction.PATIENT_LOCK, 1L));

        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void unrelatedActionIsNotCounted() {
        AuditAnomalyRule rule = lockRule();
        when(ruleRepository.findByEnabledTrue()).thenReturn(List.of(rule));

        detector.check(log(AuditAction.DOCUMENT_DELETE, 1L));

        verify(auditLogRepository, never()).countDistinctTargets(any(), any(), any(), any());
        verify(notificationService, never()).createAuditAnomalyAlert(any(), any(), any());
    }

    @Test
    void anomalyLogsAreIgnoredToAvoidLoops() {
        detector.check(log(AuditAction.ANOMALY_DETECTED, 1L));
        detector.check(log(AuditAction.ANOMALY_RULE_UPDATE, 1L));

        verify(ruleRepository, never()).findByEnabledTrue();
        verify(notificationService, never()).createAuditAnomalyAlert(any(), any(), any());
    }

    @Test
    void logsWithoutActorAreSkipped() {
        detector.check(log(AuditAction.PATIENT_LOCK, null));

        verify(ruleRepository, never()).findByEnabledTrue();
    }

    @Test
    void restoreAlwaysAlertsWithoutCheckingRules() {
        when(notificationService.createAuditAnomalyAlert(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(mock(NotificationResponse.class)));

        detector.check(log(AuditAction.SYSTEM_RESTORE, 1L));

        verify(notificationService).createAuditAnomalyAlert(
                eq("Cảnh báo: dữ liệu vừa được khôi phục"), anyString(), eq("AUDIT_ANOMALY:SYSTEM_RESTORE:99"));
        verify(ruleRepository, never()).findByEnabledTrue();
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void detectorErrorsNeverBubbleUp() {
        when(ruleRepository.findByEnabledTrue()).thenThrow(new RuntimeException("MySQL down"));

        assertDoesNotThrow(() -> detector.check(log(AuditAction.PATIENT_LOCK, 1L)));
    }
}