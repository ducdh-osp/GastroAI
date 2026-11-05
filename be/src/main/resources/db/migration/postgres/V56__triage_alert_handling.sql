ALTER TABLE triage_alerts
    ADD COLUMN outcome              VARCHAR(20),
    ADD COLUMN resolution_note      VARCHAR(1000),
    ADD COLUMN last_message_content TEXT,
    ADD COLUMN repeat_count         INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN last_repeated_at     TIMESTAMPTZ,
    ADD CONSTRAINT ck_triage_alerts_outcome CHECK (outcome IN ('HANDLED', 'FALSE_ALARM'));

CREATE TABLE triage_alert_messages (
    id          BIGSERIAL PRIMARY KEY,
    alert_id    BIGINT        NOT NULL REFERENCES triage_alerts(id) ON DELETE CASCADE,
    sender_type VARCHAR(20)   NOT NULL CHECK (sender_type IN ('PATIENT', 'DOCTOR', 'ADMIN')),
    sender_id   BIGINT        NOT NULL,
    content     VARCHAR(2000) NOT NULL,
    read_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_triage_alert_messages_alert
    ON triage_alert_messages(alert_id, created_at);

CREATE TABLE triage_alert_actions (
    id          BIGSERIAL PRIMARY KEY,
    alert_id    BIGINT       NOT NULL REFERENCES triage_alerts(id) ON DELETE CASCADE,
    action_type VARCHAR(30)  NOT NULL,
    actor_type  VARCHAR(20),
    actor_id    BIGINT,
    detail      VARCHAR(1000),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_triage_alert_actions_alert
    ON triage_alert_actions(alert_id, created_at);

-- Bo canh bao demo tu V46 (va thong bao Admin di kem) khoi man CMS.
DELETE FROM notifications WHERE dedupe_key LIKE 'DEMO30D:TRIAGE:%';
DELETE FROM triage_alerts WHERE message_content LIKE '[DEMO-30D]%';

UPDATE triage_alerts SET outcome = 'HANDLED' WHERE status = 'RESOLVED';

INSERT INTO triage_alert_actions (alert_id, action_type, created_at)
SELECT id, 'CREATED', occurred_at FROM triage_alerts;

INSERT INTO triage_alert_actions (alert_id, action_type, actor_type, actor_id, created_at)
SELECT id, 'CLAIMED', claimed_by_type, claimed_by_id, claimed_at
FROM triage_alerts WHERE claimed_at IS NOT NULL;

INSERT INTO triage_alert_actions (alert_id, action_type, actor_type, actor_id, created_at)
SELECT id, 'RESOLVED', resolved_by_type, resolved_by_id, resolved_at
FROM triage_alerts WHERE resolved_at IS NOT NULL;
