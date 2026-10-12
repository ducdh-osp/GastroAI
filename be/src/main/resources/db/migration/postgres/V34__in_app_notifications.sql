CREATE TABLE notifications (
    id                 BIGSERIAL PRIMARY KEY,
    recipient_type     VARCHAR(20) NOT NULL,
    recipient_id       BIGINT,
    notification_type  VARCHAR(40) NOT NULL,
    title              VARCHAR(160) NOT NULL,
    message            TEXT NOT NULL,
    source_type        VARCHAR(40),
    source_id          BIGINT,
    action_url         VARCHAR(255),
    dedupe_key         VARCHAR(190) UNIQUE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_notifications_recipient CHECK (
        (recipient_type = 'PATIENT' AND recipient_id IS NOT NULL)
        OR (recipient_type = 'ADMIN' AND recipient_id IS NULL)
    )
);

CREATE INDEX idx_notifications_patient_created
    ON notifications(recipient_id, created_at DESC)
    WHERE recipient_type = 'PATIENT';

CREATE INDEX idx_notifications_admin_created
    ON notifications(created_at DESC)
    WHERE recipient_type = 'ADMIN';

CREATE TABLE notification_states (
    id                 BIGSERIAL PRIMARY KEY,
    notification_id    BIGINT NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    recipient_type     VARCHAR(20) NOT NULL,
    recipient_id       BIGINT NOT NULL,
    read_at            TIMESTAMPTZ,
    deleted_at         TIMESTAMPTZ,
    CONSTRAINT uq_notification_state_recipient
        UNIQUE(notification_id, recipient_type, recipient_id)
);

CREATE INDEX idx_notification_states_recipient
    ON notification_states(recipient_type, recipient_id, notification_id);
