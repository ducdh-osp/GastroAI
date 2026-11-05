package vn.gastroai.be.api.patient;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.api.cms.triage.TriageAlertMessageRequest;
import vn.gastroai.be.api.support.AuthenticatedRequest;
import vn.gastroai.be.application.triage.TriageAlertConversationService;

import java.security.Principal;
import java.util.List;

/** Benh nhan xem va tra loi tin nhan cua bac si trong 1 canh bao cap cuu. */
@RestController
@RequestMapping("/api/v1/patient/triage-alerts")
public class PatientTriageAlertController {

    private final TriageAlertConversationService conversationService;

    public PatientTriageAlertController(TriageAlertConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    public List<TriageAlertConversationService.PatientConversationSummary> list(
            Principal principal, Authentication authentication) {
        return conversationService.listForPatient(AuthenticatedRequest.patientId(principal, authentication));
    }

    @GetMapping("/{alertId}")
    public TriageAlertConversationService.PatientConversationView get(
            @PathVariable Long alertId,
            Principal principal, Authentication authentication) {
        return conversationService.getForPatient(
                AuthenticatedRequest.patientId(principal, authentication), alertId);
    }

    @PostMapping("/{alertId}/messages")
    public TriageAlertConversationService.PatientMessageItem reply(
            @PathVariable Long alertId,
            @Valid @RequestBody TriageAlertMessageRequest request,
            Principal principal, Authentication authentication) {
        return conversationService.replyAsPatient(
                AuthenticatedRequest.patientId(principal, authentication), alertId, request.content());
    }
}
