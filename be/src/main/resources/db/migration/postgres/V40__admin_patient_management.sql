-- Khoa thu cong tu CMS doc lap voi locked_until.
ALTER TABLE patients
    ADD COLUMN admin_locked BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN admin_locked_at TIMESTAMPTZ;

CREATE INDEX idx_patients_admin_locked ON patients (admin_locked, created_at DESC);
