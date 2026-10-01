package vn.gastroai.be.api.patient;

import vn.gastroai.be.domain.triage.SymptomAssessmentInput;

import java.util.List;

public record SymptomAssessmentResponse(
        SymptomAssessmentInput.SeverityLevel severityLevel,
        boolean emergency,
        List<String> matchedGroups,
        List<String> reasonCodes,
        boolean requiresClinicianReview) {
}
