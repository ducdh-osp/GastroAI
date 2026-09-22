ALTER TABLE patients
    ADD COLUMN password_reset_token_hash VARCHAR(64),
    ADD COLUMN password_reset_token_expires_at TIMESTAMPTZ;

CREATE UNIQUE INDEX uk_patients_password_reset_token_hash
    ON patients (password_reset_token_hash)
    WHERE password_reset_token_hash IS NOT NULL;
