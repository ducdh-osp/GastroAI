DELETE FROM patient_login_history
WHERE patient_id IN (
    SELECT id
    FROM patients
    WHERE role <> 'PATIENT'
);

DELETE FROM patients
WHERE role <> 'PATIENT';

ALTER TABLE patients
    ADD CONSTRAINT chk_patients_role_patient_only CHECK (role = 'PATIENT');
