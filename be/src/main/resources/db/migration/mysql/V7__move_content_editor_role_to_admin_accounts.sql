-- CONTENT_EDITOR is an administrative account role. Existing doctor-backed editors
-- return to DOCTOR so no account or doctor login-history row needs to be moved/deleted.
ALTER TABLE admins
    DROP CHECK chk_admins_role;

ALTER TABLE doctors
    DROP CHECK chk_doctors_role;

UPDATE doctors
SET role = 'DOCTOR'
WHERE role = 'CONTENT_EDITOR';

ALTER TABLE admins
    ADD CONSTRAINT chk_admins_role CHECK (role IN ('ADMIN', 'CONTENT_EDITOR'));

ALTER TABLE doctors
    ADD CONSTRAINT chk_doctors_role CHECK (role = 'DOCTOR');
