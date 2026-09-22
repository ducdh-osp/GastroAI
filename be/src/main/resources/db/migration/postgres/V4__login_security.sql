ALTER TABLE patients
    ADD COLUMN failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN locked_until TIMESTAMPTZ;

CREATE TABLE patient_login_history (
    id              BIGSERIAL PRIMARY KEY,
    patient_id      BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    attempted_at    TIMESTAMPTZ NOT NULL,
    outcome         VARCHAR(20) NOT NULL,
    failure_reason  VARCHAR(50),
    ip_address      VARCHAR(45) NOT NULL,
    user_agent      VARCHAR(1024) NOT NULL,
    device_label    VARCHAR(255) NOT NULL
);

CREATE INDEX idx_patient_login_history_account_time
    ON patient_login_history(patient_id, attempted_at DESC);

CREATE INDEX idx_patient_login_history_cleanup
    ON patient_login_history(attempted_at);
