-- Them tai khoan test cho du 3 vai tro RBAC (UC0047 "toi thieu 3 vai tro") - V2 truoc do chi
-- seed dung 1 admin + 1 doctor (role mac dinh DOCTOR), chua co tai khoan CONTENT_EDITOR nao va
-- chi co 1 admin duy nhat (kho test cac truong hop can >=2 admin hoac >=2 doctor cung luc).
-- Dung chung 1 mat khau "1" voi V2 cho de nho luc test (bcrypt hash giong het V2).
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
    ('doctor3@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Doctor Ba GastroAI', 'DOCTOR', NOW(), NOW()),
    ('editor@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Content Editor Mot GastroAI', 'CONTENT_EDITOR', NOW(), NOW()),
    ('editor2@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Content Editor Hai GastroAI', 'CONTENT_EDITOR', NOW(), NOW());
