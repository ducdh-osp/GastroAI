-- Them tai khoan test de co >=2 admin / >=2 doctor cung luc (V2 truoc do chi seed dung 1 admin
-- + 1 doctor, kho test cac truong hop can nhieu tai khoan cung loai - vd doi role, khoa tai
-- khoan nguoi khac). Dung chung 1 mat khau "1" voi V2 cho de nho luc test (bcrypt hash giong
-- het V2).
INSERT INTO admins (email, password_hash, full_name, created_at, updated_at)
VALUES (
    'admin2@gastroai.vn',
    '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG',
    'Admin Hai GastroAI',
    NOW(),
    NOW()
);

INSERT INTO doctors (email, password_hash, full_name, role, created_at, updated_at)
VALUES
    ('doctor2@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Doctor Hai GastroAI', 'DOCTOR', NOW(), NOW()),
    ('doctor3@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Doctor Ba GastroAI', 'DOCTOR', NOW(), NOW());
