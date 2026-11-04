CREATE TABLE patient_password_history (
    id            BIGSERIAL PRIMARY KEY,
    patient_id    BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_patient_password_history_patient
    ON patient_password_history(patient_id, created_at DESC);
