CREATE TABLE medication_reminder_times (
    reminder_id BIGINT NOT NULL REFERENCES medication_reminders(id) ON DELETE CASCADE,
    time_of_day TIME NOT NULL,
    sort_order INTEGER NOT NULL,
    PRIMARY KEY (reminder_id, sort_order),
    UNIQUE (reminder_id, time_of_day)
);

INSERT INTO medication_reminder_times(reminder_id, time_of_day, sort_order)
SELECT id, time_of_day, 0 FROM medication_reminders;

ALTER TABLE medication_reminders
    ADD COLUMN start_date DATE,
    ADD COLUMN end_date DATE,
    ADD COLUMN instructions TEXT,
    ADD CONSTRAINT ck_medication_date_range
        CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date);

DROP INDEX IF EXISTS uq_medication_confirmations_reminder_date;

ALTER TABLE medication_confirmations
    ADD COLUMN scheduled_time TIME;

UPDATE medication_confirmations confirmation
SET scheduled_time = reminder.time_of_day
FROM medication_reminders reminder
WHERE confirmation.reminder_id = reminder.id;

CREATE UNIQUE INDEX uq_medication_confirmations_reminder_date_time
    ON medication_confirmations(reminder_id, confirmation_date, scheduled_time)
    WHERE confirmation_date IS NOT NULL AND scheduled_time IS NOT NULL;

ALTER TABLE medication_reminders DROP COLUMN time_of_day;
