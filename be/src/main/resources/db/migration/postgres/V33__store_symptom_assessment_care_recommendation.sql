ALTER TABLE symptom_assessments
    ADD COLUMN care_recommendation VARCHAR(30);

UPDATE symptom_assessments
SET care_recommendation = CASE
    WHEN emergency THEN 'EMERGENCY_CARE'
    WHEN requires_clinician_review OR severity_level = 'SEVERE' THEN 'SEE_DOCTOR'
    WHEN (severity_level = 'MODERATE'
            AND (activity_impact = 'PREVENTS_NORMAL_ACTIVITY' OR progression = 'WORSENING'))
        OR duration IN ('MORE_THAN_THREE_DAYS', 'RECURRING') THEN 'SEE_DOCTOR'
    ELSE 'SELF_CARE_AT_HOME'
END
WHERE care_recommendation IS NULL;

ALTER TABLE symptom_assessments
    ALTER COLUMN care_recommendation SET NOT NULL;
