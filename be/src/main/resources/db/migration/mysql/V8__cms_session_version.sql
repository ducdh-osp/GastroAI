ALTER TABLE admins
    ADD COLUMN session_version INT NOT NULL DEFAULT 0;

ALTER TABLE doctors
    ADD COLUMN session_version INT NOT NULL DEFAULT 0;