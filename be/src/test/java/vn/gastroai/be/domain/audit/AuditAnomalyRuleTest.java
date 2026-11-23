package vn.gastroai.be.domain.audit;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditAnomalyRuleTest {

    private AuditAnomalyRule rule(List<AuditAction> actions, AuditAnomalyRule.Metric metric) {
        AuditAnomalyRule rule = new AuditAnomalyRule();
        ReflectionTestUtils.setField(rule, "code", "TEST_RULE");
        ReflectionTestUtils.setField(rule, "actions", new ArrayList<>(actions));
        ReflectionTestUtils.setField(rule, "metric", metric);
        ReflectionTestUtils.setField(rule, "threshold", 5);
        ReflectionTestUtils.setField(rule, "windowMinutes", 10);
        ReflectionTestUtils.setField(rule, "enabled", true);
        return rule;
    }

    @Test
    void returnsConfiguredActions() {
        AuditAnomalyRule rule = rule(List.of(AuditAction.DOCUMENT_DELETE, AuditAction.DOCUMENT_UNPUBLISH),
                AuditAnomalyRule.Metric.ACTIONS);

        assertEquals(List.of(AuditAction.DOCUMENT_DELETE, AuditAction.DOCUMENT_UNPUBLISH), rule.getActionList());
    }

    @Test
    void ruleWithoutActionsGivesEmptyList() {
        AuditAnomalyRule rule = rule(List.of(), AuditAnomalyRule.Metric.DISTINCT_IPS);

        assertTrue(rule.getActionList().isEmpty());
    }

    @Test
    void appliesOnlyToConfiguredActions() {
        AuditAnomalyRule rule = rule(List.of(AuditAction.PATIENT_LOCK, AuditAction.PATIENT_UNLOCK),
                AuditAnomalyRule.Metric.DISTINCT_TARGETS);

        assertTrue(rule.appliesTo(AuditAction.PATIENT_LOCK));
        assertTrue(rule.appliesTo(AuditAction.PATIENT_UNLOCK));
        assertFalse(rule.appliesTo(AuditAction.DOCUMENT_DELETE));
    }

    @Test
    void multiIpRuleAppliesToEveryAction() {
        AuditAnomalyRule rule = rule(List.of(), AuditAnomalyRule.Metric.DISTINCT_IPS);

        assertTrue(rule.appliesTo(AuditAction.DOCUMENT_DELETE));
        assertTrue(rule.appliesTo(AuditAction.PATIENT_DATA_VIEW));
    }

    @Test
    void updateChangesEditableFieldsAndStampsAuthor() {
        AuditAnomalyRule rule = rule(List.of(AuditAction.RBAC_ROLE_CHANGE), AuditAnomalyRule.Metric.DISTINCT_TARGETS);

        rule.update(15, 30, false, "admin@gastroai.vn");

        assertEquals(15, rule.getThreshold());
        assertEquals(30, rule.getWindowMinutes());
        assertFalse(rule.isEnabled());
        assertEquals("admin@gastroai.vn", rule.getUpdatedBy());
        assertNotNull(rule.getUpdatedAt());
        assertEquals("TEST_RULE", rule.getCode());
    }
}