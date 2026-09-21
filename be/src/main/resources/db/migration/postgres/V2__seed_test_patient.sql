-- Seed 1 tài khoản test để thử UC0002 (đăng nhập) — chưa có UC0001 (đăng ký).
-- Email: test@gastroai.vn / Mật khẩu: Passw0rd!
INSERT INTO patients (email, password_hash, full_name) VALUES (
    'test@gastroai.vn',
    '$2a$10$J/DOi7aiZ88yGWYUH.wi7OdHig/.KoUaLZl8I3AhN24Svzr8MX8te',
    'Bệnh nhân Test'
);
