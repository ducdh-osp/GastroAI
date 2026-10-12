-- UC0009/UC0010: ho so benh ly ca nhan cua benh nhan (tien su benh, di ung, thuoc dang
-- dung) - dung de AI tham khao khi tu van, giup cau tra loi sat hon voi tinh trang thuc
-- te tung nguoi. 1 dong/1 benh nhan (patient_id UNIQUE) - khai bao (UC0009) va cap nhat
-- (UC0010) deu la UPSERT vao cung 1 ban ghi, khong tach rieng lich su thay doi.
--
-- allergies/current_medications luu JSON array of strings (giong cach chat_messages.sources/
-- related_questions dang luu) - serialize/deserialize o tang service, khong dung bang con
-- chuan hoa rieng cho tung di ung/thuoc.
CREATE TABLE medical_profiles (
    id                   BIGSERIAL PRIMARY KEY,
    patient_id           BIGINT NOT NULL UNIQUE REFERENCES patients(id) ON DELETE CASCADE,
    medical_history      TEXT,
    allergies            TEXT,
    current_medications  TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);
