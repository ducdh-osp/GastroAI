package vn.gastroai.be.api.patient;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.api.support.AuthenticatedRequest;
import vn.gastroai.be.application.audit.AuditLogService;
import vn.gastroai.be.application.triage.TriageAlertService;
import vn.gastroai.be.domain.audit.AuditAction;
import vn.gastroai.be.domain.triage.TriagePatientResponse;

import java.security.Principal;

/**
 * SCR-AD-13 buoc 1 - benh nhan tra loi "Ban co on khong?" ngay tren popup / trang Thong bao.
 * Khong co trang hoi thoai rieng: ca khan cap do Admin goi dien, khong nhan tin trong app.
 */
@RestController
@RequestMapping("/api/v1/patient/triage-alerts")
public class PatientTriageAlertController {

    private final TriageAlertService triageAlertService;
    private final AuditLogService auditLogService;

    public PatientTriageAlertController(TriageAlertService triageAlertService,
                                        AuditLogService auditLogService) {
        this.triageAlertService = triageAlertService;
        this.auditLogService = auditLogService;
    }

    /** [An toan] dong ca, [Can ho tro] chuyen "Can can thiep" ngay. */
    @PostMapping("/{alertId}/check-in")
    public TriageAlertService.CheckInResult checkIn(
            @PathVariable Long alertId,
            @Valid @RequestBody TriageCheckInRequest request,
            Principal principal, Authentication authentication) {
        Long patientId = AuthenticatedRequest.patientId(principal, authentication);
        TriageAlertService.CheckInResult result =
                triageAlertService.respondToCheckIn(patientId, alertId, request.response());
        if (!result.alreadyClosed()) {
            auditLogService.record(AuditAction.TRIAGE_ALERT_PATIENT_RESPONSE, "TRIAGE_ALERT", alertId,
                    "Bệnh nhân #" + patientId + " · "
                            + (request.response() == TriagePatientResponse.SAFE ? "An toàn" : "Cần hỗ trợ"));
        }
        return result;
    }
}
