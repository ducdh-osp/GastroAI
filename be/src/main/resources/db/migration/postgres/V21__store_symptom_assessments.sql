CREATE TABLE symptom_assessments (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    primary_symptom VARCHAR(40) NOT NULL,
    primary_symptom_detail VARCHAR(500),
    duration VARCHAR(40) NOT NULL,
    reported_severity VARCHAR(20) NOT NULL,
    activity_impact VARCHAR(40) NOT NULL,
    progression VARCHAR(20) NOT NULL,
    patient_group VARCHAR(50) NOT NULL,
    warning_signs TEXT NOT NULL,
    severity_level VARCHAR(20) NOT NULL,
    emergency BOOLEAN NOT NULL,
    matched_groups TEXT NOT NULL,
    reason_codes TEXT NOT NULL,
    requires_clinician_review BOOLEAN NOT NULL,
    assessed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_symptom_assessments_patient_assessed
    ON symptom_assessments (patient_id, assessed_at DESC);
