package vn.gastroai.be.domain.triage;

import java.util.Set;

/** Structured, patient-reported input for the first-level symptom assessment. */
public record SymptomAssessmentInput(
        PrimarySymptom primarySymptom,
        String primarySymptomDetail,
        Duration duration,
        SeverityLevel reportedSeverity,
        ActivityImpact activityImpact,
        Progression progression,
        PatientGroup patientGroup,
        Set<WarningSign> warningSigns) {

    public enum PrimarySymptom {
        ABDOMINAL_PAIN, DIARRHEA, CONSTIPATION, NAUSEA, VOMITING, HEARTBURN, BLOATING, OTHER
    }

    public enum Duration {
        LESS_THAN_24_HOURS, ONE_TO_THREE_DAYS, MORE_THAN_THREE_DAYS, RECURRING, UNSURE
    }

    public enum SeverityLevel {
        MILD, MODERATE, SEVERE, UNDETERMINED
    }

    public enum ActivityImpact {
        NONE, SOME_LIMITATION, PREVENTS_NORMAL_ACTIVITY
    }

    public enum Progression {
        IMPROVING, STABLE, WORSENING
    }

    /** The initial rule set is intended for adults; other groups need clinician-reviewed pathways. */
    public enum PatientGroup {
        ADULT, UNDER_18, PREGNANT_OR_RECENTLY_POSTPARTUM, UNSURE
    }

    /** Patient-reported warning signs based on the existing GastroAI triage groups. */
    public enum WarningSign {
        BLOOD_IN_VOMIT, BLACK_OR_BLOODY_STOOL, SUDDEN_SEVERE_ABDOMINAL_PAIN,
        RIGID_OR_TENDER_ABDOMEN, UNABLE_TO_PASS_STOOL_OR_GAS, UNABLE_TO_URINATE,
        BREATHING_DIFFICULTY_OR_CHEST_PAIN, FAINTING_OR_CONFUSION,
        HIGH_FEVER_WITH_ABDOMINAL_PAIN, PAIN_RADIATING_TO_BACK_OR_SHOULDER,
        DIABETES_WITH_VOMITING, JAUNDICE_WITH_ABDOMINAL_PAIN, SEVERE_DEHYDRATION,
        UNEXPLAINED_WEIGHT_LOSS, DIFFICULT_OR_PAINFUL_SWALLOWING
    }
}
