INSERT INTO admins (email, password_hash, full_name, created_at, updated_at)
VALUES (
    'admin@gastroai.vn',
    '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG',
    'Admin GastroAI',
    NOW(),
    NOW()
);

INSERT INTO doctors (email, password_hash, full_name, created_at, updated_at)
VALUES (
    'doctor@gastroai.vn',
    '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG',
    'Doctor GastroAI',
    NOW(),
    NOW()
);