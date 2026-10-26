-- Comprehensive CMS test accounts.
-- Shared development password for every account below: Passw0rd!
-- Admin-table accounts log in by selecting "Admin"; doctor-table accounts select "Bác sĩ".

INSERT IGNORE INTO admins (
    email, password_hash, full_name, created_at, updated_at, role, session_version
) VALUES
    ('gastroai.admin01@gmail.com', '$2a$10$J/DOi7aiZ88yGWYUH.wi7OdHig/.KoUaLZl8I3AhN24Svzr8MX8te',
     'Nguyễn Quang Minh - Quản trị viên', NOW() - INTERVAL 60 DAY, NOW(), 'ADMIN', 0),
    ('gastroai.editor01@gmail.com', '$2a$10$J/DOi7aiZ88yGWYUH.wi7OdHig/.KoUaLZl8I3AhN24Svzr8MX8te',
     'Trần Thanh Mai - Biên tập nội dung', NOW() - INTERVAL 60 DAY, NOW(), 'CONTENT_EDITOR', 0);

INSERT IGNORE INTO doctors (
    email, password_hash, full_name, created_at, updated_at, role, session_version
) VALUES
    ('gastroai.doctor01@gmail.com', '$2a$10$J/DOi7aiZ88yGWYUH.wi7OdHig/.KoUaLZl8I3AhN24Svzr8MX8te',
     'BS. Lê Hoàng Nam', NOW() - INTERVAL 60 DAY, NOW(), 'DOCTOR', 0),
    ('gastroai.doctor02@gmail.com', '$2a$10$J/DOi7aiZ88yGWYUH.wi7OdHig/.KoUaLZl8I3AhN24Svzr8MX8te',
     'BS. Phạm Ngọc Lan', NOW() - INTERVAL 60 DAY, NOW(), 'DOCTOR', 0),
    ('gastroai.doctor03@gmail.com', '$2a$10$J/DOi7aiZ88yGWYUH.wi7OdHig/.KoUaLZl8I3AhN24Svzr8MX8te',
     'BS. Võ Anh Tuấn', NOW() - INTERVAL 60 DAY, NOW(), 'DOCTOR', 0);

CREATE TEMPORARY TABLE gastroai_seed_days (
    day_offset INT NOT NULL PRIMARY KEY
);

INSERT INTO gastroai_seed_days(day_offset) VALUES
    (0), (1), (2), (3), (4), (5), (6), (7), (8), (9),
    (10), (11), (12), (13), (14), (15), (16), (17), (18), (19),
    (20), (21), (22), (23), (24), (25), (26), (27), (28), (29);

-- At least one login-history row per calendar day for both administrative accounts.
INSERT INTO admin_login_history (
    admin_id, attempted_at, outcome, failure_reason, ip_address, user_agent, device_label
)
SELECT a.id,
       DATE_ADD(DATE_SUB(CURRENT_DATE, INTERVAL d.day_offset DAY), INTERVAL (8 + MOD(a.id, 3)) HOUR),
       CASE WHEN MOD(d.day_offset + a.id, 13) = 0 THEN 'FAILURE' ELSE 'SUCCESS' END,
       CASE WHEN MOD(d.day_offset + a.id, 13) = 0 THEN 'BAD_PASSWORD' ELSE NULL END,
       CONCAT('10.30.', MOD(a.id, 200), '.', 20 + d.day_offset),
       'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/140.0 GastroAI-Seed',
       CASE WHEN MOD(a.id, 2) = 0 THEN 'Edge on Windows' ELSE 'Chrome on Windows' END
FROM admins a
CROSS JOIN gastroai_seed_days d
WHERE a.email IN ('gastroai.admin01@gmail.com', 'gastroai.editor01@gmail.com');

-- At least one login-history row per calendar day for all three doctor accounts.
INSERT INTO doctor_login_history (
    doctor_id, attempted_at, outcome, failure_reason, ip_address, user_agent, device_label
)
SELECT doctor.id,
       DATE_ADD(DATE_SUB(CURRENT_DATE, INTERVAL d.day_offset DAY), INTERVAL (7 + MOD(doctor.id, 4)) HOUR),
       CASE WHEN MOD(d.day_offset + doctor.id, 14) = 0 THEN 'FAILURE' ELSE 'SUCCESS' END,
       CASE WHEN MOD(d.day_offset + doctor.id, 14) = 0 THEN 'BAD_PASSWORD' ELSE NULL END,
       CONCAT('10.40.', MOD(doctor.id, 200), '.', 20 + d.day_offset),
       'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) Safari/605.1 GastroAI-Seed',
       CASE WHEN MOD(doctor.id, 2) = 0 THEN 'Safari on macOS' ELSE 'Chrome on Windows' END
FROM doctors doctor
CROSS JOIN gastroai_seed_days d
WHERE doctor.email IN (
    'gastroai.doctor01@gmail.com',
    'gastroai.doctor02@gmail.com',
    'gastroai.doctor03@gmail.com'
);

DROP TEMPORARY TABLE gastroai_seed_days;

