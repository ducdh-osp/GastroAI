package vn.gastroai.be.domain.triage;

import java.util.List;

public record StructuredTriageResult(
        SymptomAssessmentInput.SeverityLevel severityLevel,
        boolean emergency,
        List<String> matchedGroups,
        List<String> reasonCodes,
        boolean requiresClinicianReview,
        CareRecommendation careRecommendation) {
}
