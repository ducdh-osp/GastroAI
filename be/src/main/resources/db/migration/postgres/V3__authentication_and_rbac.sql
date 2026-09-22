ALTER TABLE patients
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'PATIENT',
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN verification_token_hash VARCHAR(64),
    ADD COLUMN verification_token_expires_at TIMESTAMPTZ,
    ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;
UPDATE patients SET email_verified = TRUE;
CREATE UNIQUE INDEX uk_patients_verification_token_hash ON patients (verification_token_hash) WHERE verification_token_hash IS NOT NULL;
CREATE TABLE revoked_tokens (jti VARCHAR(36) PRIMARY KEY, expires_at TIMESTAMPTZ NOT NULL, revoked_at TIMESTAMPTZ NOT NULL DEFAULT now());
INSERT INTO patients (email,password_hash,full_name,role,email_verified) VALUES ('doctor@gastroai.vn','$2a$10$J/DOi7aiZ88yGWYUH.wi7OdHig/.KoUaLZl8I3AhN24Svzr8MX8te','Bac si GastroAI','DOCTOR',TRUE);
