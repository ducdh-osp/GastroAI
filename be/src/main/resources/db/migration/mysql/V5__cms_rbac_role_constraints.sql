-- Keep values accepted by the Java CmsRole enum valid at the database boundary too.
ALTER TABLE admins
    ADD CONSTRAINT chk_admins_role CHECK (role = 'ADMIN');

ALTER TABLE doctors
    ADD CONSTRAINT chk_doctors_role CHECK (role IN ('DOCTOR', 'CONTENT_EDITOR'));
