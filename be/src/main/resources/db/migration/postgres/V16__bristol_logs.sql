-- UC0013/UC0014: ghi nhan tinh trang tieu hoa theo thang Bristol Stool Chart (7 muc) +
-- xem xu huong theo thoi gian. Dung INTEGER (khong phai SMALLINT) cho bristol_type de
-- khop dung kieu Java Integer mac dinh cua Hibernate - tranh loi lech kieu schema-validate
-- da gap khi lam medical_profiles (xem V14, luc do dung SMALLINT nhung entity la Integer).
CREATE TABLE bristol_logs (
    id            BIGSERIAL PRIMARY KEY,
    patient_id    BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    logged_at     TIMESTAMPTZ NOT NULL,
    bristol_type  INTEGER NOT NULL CHECK (bristol_type BETWEEN 1 AND 7),
    notes         TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_bristol_logs_patient_time
    ON bristol_logs(patient_id, logged_at DESC);
