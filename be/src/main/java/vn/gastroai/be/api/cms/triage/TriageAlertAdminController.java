package vn.gastroai.be.api.cms.triage;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.application.audit.AuditLogService;
import vn.gastroai.be.application.triage.TriageAlertConversationService;
import vn.gastroai.be.application.triage.TriageAlertDetailResponse;
import vn.gastroai.be.application.triage.TriageAlertResponse;
import vn.gastroai.be.application.triage.TriageAlertService;
import vn.gastroai.be.domain.audit.AuditAction;


@RestController
@RequestMapping("/api/v1/cms/triage-alerts")
public class TriageAlertAdminController {

    private static final String TARGET = "TRIAGE_ALERT";

    private final TriageAlertService triageAlertService;
    private final TriageAlertConversationService conversationService;
    private final AuditLogService auditLogService;

    public TriageAlertAdminController(TriageAlertService triageAlertService,
                                      TriageAlertConversationService conversationService,
                                      AuditLogService auditLogService) {
        this.triageAlertService = triageAlertService;
        this.conversationService = conversationService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public TriageAlertPageResponse list(
            @RequestParam(defaultValue = "OPEN") String view,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page >= 0 va size trong khoang 1..100");
        }
        return TriageAlertPageResponse.from(triageAlertService.list(view, page, size));
    }

    @GetMapping("/{alertId}")
    public TriageAlertDetailResponse detail(
            @PathVariable Long alertId,
            Authentication authentication) {
        return triageAlertService.getDetail(
                alertId, Long.valueOf(authentication.getName()), resolveRole(authentication));
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

    @PostMapping("/{alertId}/release")
    public TriageAlertResponse release(
            @PathVariable Long alertId,
            Authentication authentication) {

        Long actorId = Long.valueOf(authentication.getName());
        TriageAlertResponse alert = triageAlertService.release(alertId, actorId, resolveRole(authentication));
        auditLogService.record(AuditAction.TRIAGE_ALERT_RELEASE, TARGET, alertId,
                "Bệnh nhân #" + alert.patientId() + " · Đang xử lý → Mới");
        return alert;
    }

    @PostMapping("/{alertId}/messages")
    public TriageAlertDetailResponse.MessageItem sendMessage(
            @PathVariable Long alertId,
            @Valid @RequestBody TriageAlertMessageRequest request,
            Authentication authentication) {

        Long staffId = Long.valueOf(authentication.getName());
        TriageAlertConversationService.StaffMessageResult result = conversationService.sendStaffMessage(
                alertId, staffId, resolveRole(authentication), request.content());
        // Khong ghi noi dung y te vao nhat ky, chi ghi do dai.
        auditLogService.record(AuditAction.TRIAGE_ALERT_MESSAGE, TARGET, alertId,
                "Bệnh nhân #" + result.patientId() + " · " + result.message().content().length() + " ký tự");
        return result.message();
    }

    @PostMapping("/{alertId}/resolve")
    public TriageAlertResponse resolve(
            @PathVariable Long alertId,
            @Valid @RequestBody(required = false) TriageAlertResolveRequest request,
            Authentication authentication) {

        Long resolverId = Long.valueOf(authentication.getName());
        String resolverType = resolveRole(authentication);
        String note = request == null ? null : request.note();
        TriageAlertResponse alert = triageAlertService.resolve(alertId, resolverId, resolverType, note);
        auditLogService.record(AuditAction.TRIAGE_ALERT_RESOLVE, TARGET, alertId,
                describeResolve(alert, resolverId, resolverType));
        return alert;
    }

    @PostMapping("/{alertId}/false-alarm")
    public TriageAlertResponse falseAlarm(
            @PathVariable Long alertId,
            @Valid @RequestBody TriageAlertFalseAlarmRequest request,
            Authentication authentication) {

        Long actorId = Long.valueOf(authentication.getName());
        String actorType = resolveRole(authentication);
        TriageAlertResponse alert = triageAlertService.markFalseAlarm(alertId, actorId, actorType, request.reason());
        auditLogService.record(AuditAction.TRIAGE_ALERT_FALSE_ALARM, TARGET, alertId,
                describeClose(alert, actorId, actorType, "Báo nhầm/Spam"));
        return alert;
    }

    static String describeResolve(TriageAlertResponse alert, Long resolverId, String resolverType) {
        return describeClose(alert, resolverId, resolverType, "Đã xử lý");
    }

    private static String describeClose(TriageAlertResponse alert, Long resolverId, String resolverType,
                                        String closedLabel) {
        String detail = "Bệnh nhân #" + alert.patientId() + " · Đang xử lý → " + closedLabel;
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
