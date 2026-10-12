CREATE TABLE triage_alerts (
    id               BIGSERIAL PRIMARY KEY,
    patient_id       BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    session_id       BIGINT,
    message_id       BIGINT,
    message_content  TEXT NOT NULL,
    matched_groups   TEXT,
    status           VARCHAR(20) NOT NULL DEFAULT 'NEW',
    claimed_by_type  VARCHAR(20),
    claimed_by_id    BIGINT,
    claimed_at       TIMESTAMPTZ,
    resolved_at      TIMESTAMPTZ,
    occurred_at      TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_triage_alerts_patient
    ON triage_alerts(patient_id);

CREATE INDEX idx_triage_alerts_status_occurred
    ON triage_alerts(status, occurred_at DESC);