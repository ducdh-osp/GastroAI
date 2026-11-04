package vn.gastroai.be.api.cms.triage;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.application.audit.AuditLogService;
import vn.gastroai.be.application.triage.TriageAlertResponse;
import vn.gastroai.be.application.triage.TriageAlertService;
import vn.gastroai.be.domain.audit.AuditAction;

import java.util.List;


@RestController
@RequestMapping("/api/v1/cms/triage-alerts")
public class TriageAlertAdminController {

    private static final String TARGET = "TRIAGE_ALERT";

    private final TriageAlertService triageAlertService;
    private final AuditLogService auditLogService;

    public TriageAlertAdminController(TriageAlertService triageAlertService,
                                      AuditLogService auditLogService) {
        this.triageAlertService = triageAlertService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public List<TriageAlertResponse> list() {
        return triageAlertService.list("OPEN", 0, 50).getContent();
    }

    @PostMapping("/{alertId}/claim")
    public TriageAlertResponse claim(
            @PathVariable Long alertId,
            Authentication authentication) {

        Long claimerId = Long.valueOf(authentication.getName());
        String claimerType = resolveRole(authentication);
        TriageAlertResponse alert = triageAlertService.claim(alertId, claimerId, claimerType);
        auditLogService.record(AuditAction.TRIAGE_ALERT_CLAIM, TARGET, alertId,
                "Bệnh nhân #" + alert.patientId() + " · Mới → Đang xử lý");
        return alert;
    }

    @PostMapping("/{alertId}/resolve")
    public TriageAlertResponse resolve(
            @PathVariable Long alertId,
            Authentication authentication) {

        Long resolverId = Long.valueOf(authentication.getName());
        String resolverType = resolveRole(authentication);
        TriageAlertResponse alert = triageAlertService.resolve(alertId, resolverId, resolverType, null);
        auditLogService.record(AuditAction.TRIAGE_ALERT_RESOLVE, TARGET, alertId,
                describeResolve(alert, resolverId, resolverType));
        return alert;
    }

    static String describeResolve(TriageAlertResponse alert, Long resolverId, String resolverType) {
        String detail = "Bệnh nhân #" + alert.patientId() + " · Đang xử lý → Đã xử lý";
        boolean resolvedBySomeoneElse = alert.claimedById() != null
                && (!alert.claimedById().equals(resolverId) || !resolverType.equals(alert.claimedByType()));
        if (resolvedBySomeoneElse) {
            detail += " (Admin đóng hộ, người tiếp nhận: " + alert.claimedByType() + " #" + alert.claimedById() + ")";
        }
        return detail;
    }

    private String resolveRole(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if ("ROLE_DOCTOR".equals(authority.getAuthority())) {
                return "DOCTOR";
            }
        }
        return "ADMIN";
    }
}