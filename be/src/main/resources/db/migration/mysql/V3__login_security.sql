-- =========================================================
-- Login security for Admin and Doctor
-- =========================================================

ALTER TABLE admins
    ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN locked_until DATETIME NULL;

ALTER TABLE doctors
    ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN locked_until DATETIME NULL;

CREATE TABLE admin_login_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    admin_id BIGINT NOT NULL,
    attempted_at TIMESTAMP NOT NULL,
    outcome VARCHAR(50) NOT NULL,
    failure_reason VARCHAR(255),
    ip_address VARCHAR(45) NOT NULL,
    user_agent VARCHAR(1024) NOT NULL,
    device_label VARCHAR(255) NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_admin_login_history_admin
        FOREIGN KEY (admin_id)
        REFERENCES admins(id)
        ON DELETE CASCADE
);

CREATE TABLE doctor_login_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    doctor_id BIGINT NOT NULL,
    attempted_at TIMESTAMP NOT NULL,
    outcome VARCHAR(50) NOT NULL,
    failure_reason VARCHAR(255),
    ip_address VARCHAR(45) NOT NULL,
    user_agent VARCHAR(1024) NOT NULL,
    device_label VARCHAR(255) NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_doctor_login_history_doctor
        FOREIGN KEY (doctor_id)
        REFERENCES doctors(id)
        ON DELETE CASCADE
);