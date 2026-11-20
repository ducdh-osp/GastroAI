-- UC0005: tai khoan CMS khong duoc dung lai mat khau da tung dung, giong phia benh nhan.
-- Moi lan doi mat khau, ma bam cua mat khau cu duoc luu vao day de so sanh.
CREATE TABLE admin_password_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    admin_id BIGINT NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,

    PRIMARY KEY (id),
    KEY idx_admin_password_history_admin (admin_id, created_at),

    CONSTRAINT fk_admin_password_history_admin
        FOREIGN KEY (admin_id)
        REFERENCES admins(id)
        ON DELETE CASCADE
);