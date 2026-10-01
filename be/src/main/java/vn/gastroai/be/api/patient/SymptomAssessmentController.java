package vn.gastroai.be.api.patient;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.application.triage.TriageService;
import vn.gastroai.be.domain.triage.StructuredTriageResult;

@RestController
@RequestMapping("/api/v1/patient/triage")
public class SymptomAssessmentController {
    private final TriageService triageService;

    public SymptomAssessmentController(TriageService triageService) {
        this.triageService = triageService;
    }

    @PostMapping("/assessments")
    public SymptomAssessmentResponse assess(
            @Valid @RequestBody SymptomAssessmentRequest request,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException("Chua dang nhap");
        }

        StructuredTriageResult result = triageService.assess(request.toInput());
        return new SymptomAssessmentResponse(result.severityLevel(), result.emergency(),
                result.matchedGroups(), result.reasonCodes(), result.requiresClinicianReview());
    }
}
