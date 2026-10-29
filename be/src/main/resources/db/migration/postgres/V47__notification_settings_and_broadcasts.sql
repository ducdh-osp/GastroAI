-- UC0073: Cai dat thong bao ca nhan (Patient notification settings)
CREATE TABLE patient_notification_settings (
    patient_id                  BIGINT PRIMARY KEY REFERENCES patients(id) ON DELETE CASCADE,
    medication_reminder_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    journal_reminder_enabled    BOOLEAN NOT NULL DEFAULT TRUE,
    system_broadcast_enabled    BOOLEAN NOT NULL DEFAULT TRUE,
    security_alert_enabled      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- UC0075: Thong bao broadcast tu Admin toi nguoi dung (Broadcast announcements)
CREATE TABLE broadcast_announcements (
    id                   BIGSERIAL PRIMARY KEY,
    title                VARCHAR(160) NOT NULL,
    message              TEXT NOT NULL,
    target_group         VARCHAR(40) NOT NULL DEFAULT 'ALL',
    action_url           VARCHAR(255),
    created_by_admin_id  BIGINT,
    recipient_count      INT NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_broadcast_announcements_created_at
    ON broadcast_announcements(created_at DESC);
