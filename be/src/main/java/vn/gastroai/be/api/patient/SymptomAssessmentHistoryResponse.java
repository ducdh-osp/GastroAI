package vn.gastroai.be.api.patient;

import java.time.Instant;
import java.util.List;

public record SymptomAssessmentHistoryResponse(
        Long id,
        String primarySymptom,
        String primarySymptomDetail,
        String duration,
        String reportedSeverity,
        String activityImpact,
        String progression,
        String patientGroup,
        List<String> warningSigns,
        String severityLevel,
        boolean emergency,
        List<String> matchedGroups,
        List<String> reasonCodes,
        boolean requiresClinicianReview,
        Instant assessedAt) {
}
