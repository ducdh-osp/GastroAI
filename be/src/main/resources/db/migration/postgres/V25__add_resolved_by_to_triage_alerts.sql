ALTER TABLE triage_alerts
    ADD COLUMN IF NOT EXISTS resolved_by_type VARCHAR(20),
    ADD COLUMN IF NOT EXISTS resolved_by_id BIGINT;