package vn.gastroai.be.application.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;
import vn.gastroai.be.api.cms.audit.AuditAnomalyRuleResponse;
import vn.gastroai.be.api.cms.audit.UpdateAuditAnomalyRuleRequest;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.domain.audit.AuditAction;
import vn.gastroai.be.domain.audit.AuditAnomalyRule;
import vn.gastroai.be.infrastructure.persistence.mysql.AuditAnomalyRuleRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditAnomalyRuleServiceTest {

    @Mock private AuditAnomalyRuleRepository ruleRepository;
    @Mock private AuditLogService auditLogService;

    private AuditAnomalyRuleService service;

    @BeforeEach
    void setUp() {
        service = new AuditAnomalyRuleService(ruleRepository, auditLogService);
    }

    private AuditAnomalyRule rule(String code, String name) {
        AuditAnomalyRule rule = BeanUtils.instantiateClass(AuditAnomalyRule.class);
        ReflectionTestUtils.setField(rule, "code", code);
        ReflectionTestUtils.setField(rule, "name", name);
        ReflectionTestUtils.setField(rule, "actions", "PATIENT_DATA_VIEW");
        ReflectionTestUtils.setField(rule, "metric", AuditAnomalyRule.Metric.DISTINCT_TARGETS);
        ReflectionTestUtils.setField(rule, "threshold", 20);
        ReflectionTestUtils.setField(rule, "windowMinutes", 10);
        ReflectionTestUtils.setField(rule, "enabled", true);
        ReflectionTestUtils.setField(rule, "updatedAt", Instant.parse("2026-10-29T00:00:00Z"));
        return rule;
    }

    @Test
    void listReturnsRulesInRepositoryOrder() {
        when(ruleRepository.findAllByOrderByIdAsc()).thenReturn(List.of(
                rule("MASS_PATIENT_VIEW", "Xem dữ liệu nhiều bệnh nhân"),
                rule("MULTI_IP", "Thao tác từ nhiều địa chỉ IP")));

        List<AuditAnomalyRuleResponse> result = service.list();

        assertEquals(2, result.size());
        assertEquals("MASS_PATIENT_VIEW", result.get(0).code());
        assertEquals(List.of(AuditAction.PATIENT_DATA_VIEW), result.get(0).actions());
        assertEquals("MULTI_IP", result.get(1).code());
    }

    @Test
    void updateSavesNewValuesAndAuditsBeforeAndAfter() {
        AuditAnomalyRule rule = rule("MASS_PATIENT_VIEW", "Xem dữ liệu nhiều bệnh nhân");
        when(ruleRepository.findByCode("MASS_PATIENT_VIEW")).thenReturn(Optional.of(rule));
        when(ruleRepository.save(any(AuditAnomalyRule.class))).then(returnsFirstArg());
        when(auditLogService.currentActorEmail()).thenReturn("admin@gastroai.vn");

        AuditAnomalyRuleResponse response = service.update("MASS_PATIENT_VIEW",
                new UpdateAuditAnomalyRuleRequest(15, 10, false));

        assertEquals(15, response.threshold());
        assertEquals(10, response.windowMinutes());
        assertFalse(response.enabled());
        assertEquals("admin@gastroai.vn", response.updatedBy());
        verify(auditLogService).record(AuditAction.ANOMALY_RULE_UPDATE, "RULE", "MASS_PATIENT_VIEW",
                "Xem dữ liệu nhiều bệnh nhân: ngưỡng 20 / 10 phút, bật → ngưỡng 15 / 10 phút, tắt");
    }

    @Test
    void unknownCodeThrowsNotFoundAndChangesNothing() {
        when(ruleRepository.findByCode("KHONG_CO")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update("KHONG_CO",
                new UpdateAuditAnomalyRuleRequest(5, 10, true)));

        verify(ruleRepository, never()).save(any());
        verify(auditLogService, never()).record(any(), any(), any(), any());
    }
}