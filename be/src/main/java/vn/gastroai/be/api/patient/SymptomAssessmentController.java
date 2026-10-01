package vn.gastroai.be.api.patient;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.application.triage.SymptomAssessmentHistoryService;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/patient/triage")
public class SymptomAssessmentController {
    private final SymptomAssessmentHistoryService historyService;

    public SymptomAssessmentController(SymptomAssessmentHistoryService historyService) {
        this.historyService = historyService;
    }

    @PostMapping("/assessments")
    public SymptomAssessmentResponse assess(
            @Valid @RequestBody SymptomAssessmentRequest request,
            Principal principal, Authentication authentication) {
        requireAuthenticated(authentication);
        return historyService.assessAndSave(Long.valueOf(principal.getName()), request.toInput());
    }

    @GetMapping("/assessments")
    public List<SymptomAssessmentHistoryResponse> history(Principal principal, Authentication authentication) {
        requireAuthenticated(authentication);
        return historyService.listForPatient(Long.valueOf(principal.getName()));
    }

    private void requireAuthenticated(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException("Chua dang nhap");
        }
    }
}
