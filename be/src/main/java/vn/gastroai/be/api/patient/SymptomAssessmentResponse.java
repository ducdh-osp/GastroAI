package vn.gastroai.be.api.patient;

import vn.gastroai.be.domain.triage.SymptomAssessmentInput;
import vn.gastroai.be.domain.triage.CareRecommendation;

import java.util.List;
import java.time.Instant;

public record SymptomAssessmentResponse(
        Long id,
        Instant assessedAt,
        SymptomAssessmentInput.SeverityLevel severityLevel,
        boolean emergency,
        List<String> matchedGroups,
        List<String> reasonCodes,
        boolean requiresClinicianReview,
        CareRecommendation careRecommendation) {
}
