ALTER TABLE medical_profiles
    ADD COLUMN emergency_contact_name VARCHAR(150),
    ADD COLUMN emergency_contact_phone VARCHAR(20),
    ADD COLUMN emergency_contact_relationship VARCHAR(80);
