package vn.gastroai.be.api.patient;

import jakarta.validation.constraints.NotNull;
import vn.gastroai.be.domain.triage.SymptomAssessmentInput;

import java.util.Set;

public record SymptomAssessmentRequest(
        @NotNull SymptomAssessmentInput.PrimarySymptom primarySymptom,
        @NotNull SymptomAssessmentInput.Duration duration,
        @NotNull SymptomAssessmentInput.SeverityLevel reportedSeverity,
        @NotNull SymptomAssessmentInput.ActivityImpact activityImpact,
        @NotNull SymptomAssessmentInput.Progression progression,
        @NotNull SymptomAssessmentInput.PatientGroup patientGroup,
        @NotNull Set<SymptomAssessmentInput.@NotNull WarningSign> warningSigns) {

    public SymptomAssessmentInput toInput() {
        return new SymptomAssessmentInput(primarySymptom, duration, reportedSeverity,
                activityImpact, progression, patientGroup, warningSigns);
    }
}
