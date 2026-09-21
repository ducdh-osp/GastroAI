-- PostgreSQL — dữ liệu bệnh nhân (mục 6 đề cương)
-- Phạm vi: UC0002 (đăng nhập), chỉ đủ cột cần cho login

CREATE TABLE patients (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
