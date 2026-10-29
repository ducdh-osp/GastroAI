package vn.gastroai.be.application.audit;

import org.springframework.stereotype.Service;
import vn.gastroai.be.api.cms.audit.AuditAnomalyRuleResponse;
import vn.gastroai.be.api.cms.audit.UpdateAuditAnomalyRuleRequest;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.domain.audit.AuditAction;
import vn.gastroai.be.domain.audit.AuditAnomalyRule;
import vn.gastroai.be.infrastructure.persistence.mysql.AuditAnomalyRuleRepository;

import java.util.List;

/** UC0063 - Admin xem va dat nguong cho cac quy tac canh bao bat thuong. */
@Service
public class AuditAnomalyRuleService {

    private final AuditAnomalyRuleRepository ruleRepository;
    private final AuditLogService auditLogService;

    public AuditAnomalyRuleService(AuditAnomalyRuleRepository ruleRepository,
                                   AuditLogService auditLogService) {
        this.ruleRepository = ruleRepository;
        this.auditLogService = auditLogService;
    }

    public List<AuditAnomalyRuleResponse> list() {
        return ruleRepository.findAllByOrderByIdAsc().stream()
                .map(AuditAnomalyRuleResponse::from)
                .toList();
    }

    public AuditAnomalyRuleResponse update(String code, UpdateAuditAnomalyRuleRequest request) {
        AuditAnomalyRule rule = ruleRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quy tắc " + code));

        String before = describe(rule.getThreshold(), rule.getWindowMinutes(), rule.isEnabled());

        rule.update(request.threshold(), request.windowMinutes(), request.enabled(),
                  auditLogService.currentActorEmail());
        AuditAnomalyRule saved = ruleRepository.save(rule);

        String after = describe(saved.getThreshold(), saved.getWindowMinutes(), saved.isEnabled());
        auditLogService.record(AuditAction.ANOMALY_RULE_UPDATE, "RULE", saved.getCode(),
                saved.getName() + ": " + before + " → " + after);

        return AuditAnomalyRuleResponse.from(saved);
    }

    private String describe(int threshold, int windowMinutes, boolean enabled) {
        return "ngưỡng " + threshold + " / " + windowMinutes + " phút, " + (enabled ? "bật" : "tắt");
    }
}