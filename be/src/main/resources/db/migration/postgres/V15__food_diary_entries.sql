-- UC0011/UC0012: nhat ky an uong hang ngay cua benh nhan (CRUD) + xem lai theo thoi gian/
-- bieu do. eaten_at la thoi diem THAT SU an (khong phai luc tao ban ghi) vi nguoi dung co
-- the ghi lai sau, nen khac voi created_at.
CREATE TABLE food_diary_entries (
    id          BIGSERIAL PRIMARY KEY,
    patient_id  BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    eaten_at    TIMESTAMPTZ NOT NULL,
    description TEXT NOT NULL,
    notes       TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_food_diary_entries_patient_time
    ON food_diary_entries(patient_id, eaten_at DESC);
