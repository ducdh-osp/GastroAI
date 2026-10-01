package vn.gastroai.be.api.cms.triage;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.api.patient.SymptomAssessmentHistoryResponse;
import vn.gastroai.be.application.triage.SymptomAssessmentHistoryService;

import java.util.List;

/** Staff read-only access to a patient's saved triage assessments. */
@RestController
@RequestMapping("/api/v1/cms/triage-assessments")
public class SymptomAssessmentAdminController {
    private final SymptomAssessmentHistoryService historyService;

    public SymptomAssessmentAdminController(SymptomAssessmentHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/patients/{patientId}")
    public List<SymptomAssessmentHistoryResponse> listForPatient(
            @PathVariable Long patientId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException("Chua dang nhap");
        }
        return historyService.listForPatient(patientId);
    }
}
