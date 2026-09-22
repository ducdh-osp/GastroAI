ALTER TABLE admin_accounts
    ADD COLUMN password_reset_token_hash VARCHAR(64),
    ADD COLUMN password_reset_token_expires_at TIMESTAMP(6) NULL;

CREATE UNIQUE INDEX uk_admin_password_reset_token_hash
    ON admin_accounts (password_reset_token_hash);
