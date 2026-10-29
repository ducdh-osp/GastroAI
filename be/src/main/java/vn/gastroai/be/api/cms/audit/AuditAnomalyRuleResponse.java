package vn.gastroai.be.api.cms.audit;

import vn.gastroai.be.domain.audit.AuditAction;
import vn.gastroai.be.domain.audit.AuditAnomalyRule;

import java.time.Instant;
import java.util.List;

public record AuditAnomalyRuleResponse(
        String code,
        String name,
        List<AuditAction> actions,
        AuditAnomalyRule.Metric metric,
        int threshold,
        int windowMinutes,
        boolean enabled,
        Instant updatedAt,
        String updatedBy
) {
    public static AuditAnomalyRuleResponse from(AuditAnomalyRule rule) {
        return new AuditAnomalyRuleResponse(
                rule.getCode(),
                rule.getName(),
                rule.getActionList(),
                rule.getMetric(),
                rule.getThreshold(),
                rule.getWindowMinutes(),
                rule.isEnabled(),
                rule.getUpdatedAt(),
                rule.getUpdatedBy());
    }
}