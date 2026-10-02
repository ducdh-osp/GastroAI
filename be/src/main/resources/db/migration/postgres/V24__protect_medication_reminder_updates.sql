-- Reject stale reminder updates instead of silently overwriting another tab's changes.
ALTER TABLE medication_reminders
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
