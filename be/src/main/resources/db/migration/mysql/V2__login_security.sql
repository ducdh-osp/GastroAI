ALTER TABLE admin_accounts
    ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN locked_until TIMESTAMP(6) NULL;

CREATE TABLE admin_login_history (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_id        BIGINT NOT NULL,
    attempted_at    TIMESTAMP(6) NOT NULL,
    outcome         VARCHAR(20) NOT NULL,
    failure_reason  VARCHAR(50),
    ip_address      VARCHAR(45) NOT NULL,
    user_agent      VARCHAR(1024) NOT NULL,
    device_label    VARCHAR(255) NOT NULL,
    CONSTRAINT fk_admin_login_history_account
        FOREIGN KEY (admin_id) REFERENCES admin_accounts(id) ON DELETE CASCADE,
    INDEX idx_admin_login_history_account_time (admin_id, attempted_at DESC),
    INDEX idx_admin_login_history_cleanup (attempted_at)
);
