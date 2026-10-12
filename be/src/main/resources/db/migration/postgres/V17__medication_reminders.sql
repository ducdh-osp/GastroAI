-- UC0015: lich nhac uong thuoc (ten thuoc, lieu luong, 1 khung gio lap lai hang ngay).
-- UC0016: xac nhan da uong - GD4 CHI can bang ghi nhan + nut "da uong" test thu cong, KHONG
-- can logic "due today"/dedupe theo ngay hay tich hop notification/scheduler that (viec do
-- la GD7, ngoai pham vi lan nay - xem ghi chu trong masterplan).
CREATE TABLE medication_reminders (
    id            BIGSERIAL PRIMARY KEY,
    patient_id    BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    medicine_name VARCHAR(200) NOT NULL,
    dosage        VARCHAR(200),
    time_of_day   TIME NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_medication_reminders_patient
    ON medication_reminders(patient_id);

-- Moi dong = 1 lan bam nut "da uong" - chi la log don gian, khong co rang buoc unique theo
-- ngay (benh nhan co the bam nhieu lan/ngay neu uong nhieu lan, GD4 khong gioi han).
CREATE TABLE medication_confirmations (
    id            BIGSERIAL PRIMARY KEY,
    reminder_id   BIGINT NOT NULL REFERENCES medication_reminders(id) ON DELETE CASCADE,
    confirmed_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_medication_confirmations_reminder_time
    ON medication_confirmations(reminder_id, confirmed_at DESC);
