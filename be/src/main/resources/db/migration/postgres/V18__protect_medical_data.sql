-- Prevent stale medical-profile forms from silently overwriting newer patient data.
ALTER TABLE medical_profiles
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- One reminder represents one daily dose/time slot. Keep all legacy confirmations, even
-- when a reminder had duplicate confirmations on the same day. Only one legacy row per
-- reminder/day receives confirmation_date; duplicates remain queryable with NULL here.
ALTER TABLE medication_confirmations
    ADD COLUMN confirmation_date DATE;

WITH ranked AS (
    SELECT id,
           (confirmed_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date AS local_date,
           row_number() OVER (
               PARTITION BY reminder_id, (confirmed_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date
               ORDER BY confirmed_at, id
           ) AS row_number
    FROM medication_confirmations
)
UPDATE medication_confirmations AS confirmation
SET confirmation_date = ranked.local_date
FROM ranked
WHERE confirmation.id = ranked.id
  AND ranked.row_number = 1;

CREATE UNIQUE INDEX uq_medication_confirmations_reminder_date
    ON medication_confirmations(reminder_id, confirmation_date)
    WHERE confirmation_date IS NOT NULL;
