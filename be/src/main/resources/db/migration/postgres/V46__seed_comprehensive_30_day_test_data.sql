-- Comprehensive test data for patient-facing features.
-- All generated accounts use the shared development password: Passw0rd!
-- The date series is relative to CURRENT_DATE so a freshly built database always has
-- 30 recent calendar days available for charts, filters and pagination tests.

INSERT INTO patients (
    email, password_hash, full_name, created_at, updated_at, role, email_verified, phone
) VALUES
    ('gastroai.patient01@gmail.com', '$2a$10$J/DOi7aiZ88yGWYUH.wi7OdHig/.KoUaLZl8I3AhN24Svzr8MX8te', 'Nguyễn Minh An', now() - interval '60 days', now(), 'PATIENT', true, '0901000001'),
    ('gastroai.patient02@gmail.com', '$2a$10$J/DOi7aiZ88yGWYUH.wi7OdHig/.KoUaLZl8I3AhN24Svzr8MX8te', 'Trần Thu Bình', now() - interval '60 days', now(), 'PATIENT', true, '0901000002'),
    ('gastroai.patient03@gmail.com', '$2a$10$J/DOi7aiZ88yGWYUH.wi7OdHig/.KoUaLZl8I3AhN24Svzr8MX8te', 'Lê Hoàng Chi', now() - interval '60 days', now(), 'PATIENT', true, '0901000003'),
    ('gastroai.patient04@gmail.com', '$2a$10$J/DOi7aiZ88yGWYUH.wi7OdHig/.KoUaLZl8I3AhN24Svzr8MX8te', 'Phạm Gia Dũng', now() - interval '60 days', now(), 'PATIENT', true, '0901000004'),
    ('gastroai.patient05@gmail.com', '$2a$10$J/DOi7aiZ88yGWYUH.wi7OdHig/.KoUaLZl8I3AhN24Svzr8MX8te', 'Võ Ngọc Hà', now() - interval '60 days', now(), 'PATIENT', true, '0901000005')
ON CONFLICT (email) DO NOTHING;

WITH profile_data(email, medical_history, allergies, current_medications, date_of_birth,
                  gender, height_cm, weight_kg, chronic_conditions, past_surgeries,
                  dietary_restrictions) AS (
    VALUES
        ('gastroai.patient01@gmail.com', 'Trào ngược dạ dày tái phát khi ăn muộn.', '["Hải sản"]',
         '["Omeprazole 20 mg"]', DATE '1992-03-12', 'MALE', 170, 66,
         '["GERD"]', '[]', '["Hạn chế đồ cay","Không uống cà phê buổi tối"]'),
        ('gastroai.patient02@gmail.com', 'Hội chứng ruột kích thích thể táo bón.', '[]',
         '["Psyllium 5 g"]', DATE '1988-07-24', 'FEMALE', 158, 53,
         '["IBS-C"]', '["Mổ ruột thừa năm 2010"]', '["Ăn nhiều chất xơ"]'),
        ('gastroai.patient03@gmail.com', 'Không dung nạp lactose mức độ nhẹ.', '["Penicillin"]',
         '["Men vi sinh"]', DATE '1997-11-06', 'FEMALE', 162, 55,
         '["Không dung nạp lactose"]', '[]', '["Không dùng sữa bò"]'),
        ('gastroai.patient04@gmail.com', 'Tiền sử viêm dạ dày do H. pylori đã điều trị.', '[]',
         '["Famotidine 20 mg"]', DATE '1984-01-19', 'MALE', 174, 72,
         '["Viêm dạ dày"]', '[]', '["Hạn chế rượu bia"]'),
        ('gastroai.patient05@gmail.com', 'Thường đầy bụng khi căng thẳng.', '["Đậu phộng"]',
         '["Simethicone 80 mg"]', DATE '2000-09-30', 'FEMALE', 160, 50,
         '["Đầy bụng chức năng"]', '[]', '["Chia nhỏ bữa ăn"]')
)
INSERT INTO medical_profiles (
    patient_id, medical_history, allergies, current_medications, created_at, updated_at,
    date_of_birth, gender, height_cm, weight_kg, chronic_conditions, past_surgeries,
    dietary_restrictions
)
SELECT p.id, d.medical_history, d.allergies, d.current_medications,
       now() - interval '45 days', now(), d.date_of_birth, d.gender, d.height_cm,
       d.weight_kg, d.chronic_conditions, d.past_surgeries, d.dietary_restrictions
FROM profile_data d
JOIN patients p ON p.email = d.email
ON CONFLICT (patient_id) DO NOTHING;

-- One login event per patient per day, including occasional failures for security UI tests.
WITH seed_patients AS (
    SELECT id, email, row_number() OVER (ORDER BY email)::integer AS patient_no
    FROM patients
    WHERE email LIKE 'gastroai.patient%@gmail.com'
), days AS (
    SELECT generate_series(0, 29)::integer AS day_offset
)
INSERT INTO patient_login_history (
    patient_id, attempted_at, outcome, failure_reason, ip_address, user_agent, device_label
)
SELECT p.id,
       (CURRENT_DATE - d.day_offset + TIME '08:00') + make_interval(mins => p.patient_no * 3),
       CASE WHEN (d.day_offset + p.patient_no) % 11 = 0 THEN 'FAILURE' ELSE 'SUCCESS' END,
       CASE WHEN (d.day_offset + p.patient_no) % 11 = 0 THEN 'BAD_PASSWORD' END,
       '10.20.' || p.patient_no || '.' || (20 + d.day_offset),
       'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/140.0 GastroAI-Seed',
       CASE WHEN p.patient_no % 2 = 0 THEN 'Edge on Windows' ELSE 'Chrome on Windows' END
FROM seed_patients p
CROSS JOIN days d;

-- Three meals per day per patient: 450 rows spanning the latest 30 calendar days.
WITH seed_patients AS (
    SELECT id, email, row_number() OVER (ORDER BY email)::integer AS patient_no
    FROM patients
    WHERE email LIKE 'gastroai.patient%@gmail.com'
), days AS (
    SELECT generate_series(0, 29)::integer AS day_offset
), meals(meal_type, meal_time, description) AS (
    VALUES
        ('BREAKFAST', TIME '07:30', 'Cháo yến mạch, chuối và sữa hạt'),
        ('LUNCH',     TIME '12:15', 'Cơm, cá hấp, rau luộc và canh bí'),
        ('DINNER',    TIME '18:30', 'Cơm mềm, thịt gà và rau củ hấp')
)
INSERT INTO food_diary_entries (
    patient_id, eaten_at, description, notes, created_at, updated_at, meal_type,
    symptoms_after_meal, symptom_onset_minutes
)
SELECT p.id,
       CURRENT_DATE - d.day_offset + m.meal_time,
       m.description || ' - mẫu ngày ' || (30 - d.day_offset),
       CASE
           WHEN m.meal_type = 'DINNER' AND d.day_offset % 7 = 0 THEN 'Ăn hơi muộn do công việc.'
           WHEN m.meal_type = 'BREAKFAST' THEN 'Uống thêm một cốc nước ấm.'
           ELSE NULL
       END,
       CURRENT_DATE - d.day_offset + m.meal_time + interval '20 minutes',
       CURRENT_DATE - d.day_offset + m.meal_time + interval '20 minutes',
       m.meal_type,
       CASE
           WHEN m.meal_type = 'DINNER' AND (d.day_offset + p.patient_no) % 6 = 0 THEN 'Đầy bụng nhẹ'
           WHEN m.meal_type = 'LUNCH' AND (d.day_offset + p.patient_no) % 9 = 0 THEN 'Ợ nóng nhẹ'
           ELSE NULL
       END,
       CASE
           WHEN m.meal_type = 'DINNER' AND (d.day_offset + p.patient_no) % 6 = 0 THEN 45
           WHEN m.meal_type = 'LUNCH' AND (d.day_offset + p.patient_no) % 9 = 0 THEN 30
           ELSE NULL
       END
FROM seed_patients p
CROSS JOIN days d
CROSS JOIN meals m;

-- One Bristol observation per patient per day with mostly normal values and periodic outliers.
WITH seed_patients AS (
    SELECT id, email, row_number() OVER (ORDER BY email)::integer AS patient_no
    FROM patients
    WHERE email LIKE 'gastroai.patient%@gmail.com'
), days AS (
    SELECT generate_series(0, 29)::integer AS day_offset
)
INSERT INTO bristol_logs (
    patient_id, logged_at, bristol_type, notes, created_at, updated_at
)
SELECT p.id,
       CURRENT_DATE - d.day_offset + TIME '07:00',
       CASE
           WHEN (d.day_offset + p.patient_no) % 13 = 0 THEN 2
           WHEN (d.day_offset + p.patient_no) % 17 = 0 THEN 6
           ELSE 3 + ((d.day_offset + p.patient_no) % 3)
       END,
       CASE
           WHEN (d.day_offset + p.patient_no) % 13 = 0 THEN 'Phân hơi cứng, cần tăng nước và chất xơ.'
           WHEN (d.day_offset + p.patient_no) % 17 = 0 THEN 'Phân lỏng sau bữa ăn nhiều dầu mỡ.'
           ELSE 'Theo dõi hằng ngày.'
       END,
       CURRENT_DATE - d.day_offset + TIME '07:05',
       CURRENT_DATE - d.day_offset + TIME '07:05'
FROM seed_patients p
CROSS JOIN days d;

-- Two reminder regimens per patient; the probiotic has two dose times per day.
WITH seed_patients AS (
    SELECT id
    FROM patients
    WHERE email LIKE 'gastroai.patient%@gmail.com'
), reminder_data(medicine_name, dosage, instructions) AS (
    VALUES
        ('Omeprazole', '20 mg', 'Uống trước bữa sáng 30 phút theo hướng dẫn của bác sĩ.'),
        ('Men vi sinh', '1 gói', 'Uống sau bữa ăn và bảo quản theo hướng dẫn trên bao bì.')
)
INSERT INTO medication_reminders (
    patient_id, medicine_name, dosage, active, created_at, updated_at,
    start_date, end_date, instructions
)
SELECT p.id, r.medicine_name, r.dosage, true,
       now() - interval '45 days', now(), CURRENT_DATE - 45, CURRENT_DATE + 30, r.instructions
FROM seed_patients p
CROSS JOIN reminder_data r;

INSERT INTO medication_reminder_times (reminder_id, time_of_day, sort_order)
SELECT r.id, t.time_of_day, t.sort_order
FROM medication_reminders r
JOIN patients p ON p.id = r.patient_id AND p.email LIKE 'gastroai.patient%@gmail.com'
CROSS JOIN LATERAL (
    SELECT TIME '07:00' AS time_of_day, 0 AS sort_order
    WHERE r.medicine_name = 'Omeprazole'
    UNION ALL
    SELECT TIME '08:00', 0 WHERE r.medicine_name = 'Men vi sinh'
    UNION ALL
    SELECT TIME '20:00', 1 WHERE r.medicine_name = 'Men vi sinh'
) t
ON CONFLICT DO NOTHING;

WITH days AS (
    SELECT generate_series(0, 29)::integer AS day_offset
)
INSERT INTO medication_confirmations (
    reminder_id, confirmed_at, confirmation_date, scheduled_time
)
SELECT r.id,
       CURRENT_DATE - d.day_offset + t.time_of_day
           + make_interval(mins => 3 + ((d.day_offset + t.sort_order) % 12)),
       CURRENT_DATE - d.day_offset,
       t.time_of_day
FROM medication_reminders r
JOIN patients p ON p.id = r.patient_id AND p.email LIKE 'gastroai.patient%@gmail.com'
JOIN medication_reminder_times t ON t.reminder_id = r.id
CROSS JOIN days d
-- Leave some doses unconfirmed so adherence reports contain realistic gaps.
WHERE (d.day_offset + p.id + t.sort_order) % 7 <> 0
ON CONFLICT DO NOTHING;

-- Six conversations per patient distributed across the 30-day period.
WITH seed_patients AS (
    SELECT id
    FROM patients
    WHERE email LIKE 'gastroai.patient%@gmail.com'
), session_topics(seq, day_offset, title) AS (
    VALUES
        (1, 29, '[DEMO-30D-01] Thực phẩm phù hợp khi ợ nóng'),
        (2, 23, '[DEMO-30D-02] Cách giảm đầy bụng'),
        (3, 17, '[DEMO-30D-03] Theo dõi táo bón'),
        (4, 11, '[DEMO-30D-04] Uống thuốc đúng giờ'),
        (5,  5, '[DEMO-30D-05] Chế độ ăn ít dầu mỡ'),
        (6,  0, '[DEMO-30D-06] Khi nào cần đi khám')
)
INSERT INTO chat_sessions (patient_id, title, created_at, updated_at)
SELECT p.id, t.title,
       CURRENT_DATE - t.day_offset + TIME '20:00',
       CURRENT_DATE - t.day_offset + TIME '20:10'
FROM seed_patients p
CROSS JOIN session_topics t;

WITH session_topics(title, question, answer) AS (
    VALUES
        ('[DEMO-30D-01] Thực phẩm phù hợp khi ợ nóng',
         'Tôi hay ợ nóng sau bữa tối, nên lựa chọn món ăn như thế nào?',
         'Bạn nên ưu tiên khẩu phần nhỏ, ít dầu mỡ và tránh nằm ngay sau khi ăn. Nếu triệu chứng kéo dài hoặc nặng hơn, hãy đi khám.'),
        ('[DEMO-30D-02] Cách giảm đầy bụng',
         'Tôi thường đầy bụng vào cuối ngày, có cách nào theo dõi nguyên nhân không?',
         'Bạn có thể ghi nhật ký món ăn, thời điểm xuất hiện triệu chứng và mức độ khó chịu để nhận biết thực phẩm liên quan.'),
        ('[DEMO-30D-03] Theo dõi táo bón',
         'Tôi nên theo dõi táo bón bằng những thông tin nào?',
         'Hãy ghi số lần đi tiêu, dạng phân Bristol, lượng nước uống, chất xơ và mức độ vận động mỗi ngày.'),
        ('[DEMO-30D-04] Uống thuốc đúng giờ',
         'Tôi thường quên liều thuốc buổi tối, nên làm gì?',
         'Bạn có thể bật lịch nhắc và xác nhận từng liều sau khi uống. Không tự uống bù nếu chưa có hướng dẫn của bác sĩ hoặc dược sĩ.'),
        ('[DEMO-30D-05] Chế độ ăn ít dầu mỡ',
         'Gợi ý cho tôi một bữa tối ít dầu mỡ và dễ tiêu.',
         'Một lựa chọn đơn giản là cơm mềm, cá hoặc thịt gà hấp, rau củ luộc và canh ít gia vị.'),
        ('[DEMO-30D-06] Khi nào cần đi khám',
         'Triệu chứng tiêu hóa nào cần đi khám sớm?',
         'Bạn nên đi khám sớm khi triệu chứng kéo dài, ảnh hưởng sinh hoạt hoặc có dấu hiệu cảnh báo như đau dữ dội, chảy máu, sốt cao hay mất nước.')
)
INSERT INTO chat_messages (
    session_id, sender, content, sources, related_questions, emergency, created_at, matched_groups
)
SELECT s.id, 'patient', t.question, NULL, NULL, false, s.created_at + interval '1 minute', NULL
FROM chat_sessions s
JOIN patients p ON p.id = s.patient_id AND p.email LIKE 'gastroai.patient%@gmail.com'
JOIN session_topics t ON t.title = s.title
UNION ALL
SELECT s.id, 'assistant', t.answer,
       '[{"documentTitle":"GastroAI - tài liệu kiến thức mẫu","snippet":"Thông tin tham khảo phục vụ kiểm thử dữ liệu 30 ngày.","sourceUrl":null}]',
       '["Tôi nên ghi nhật ký trong bao lâu?","Khi nào cần gặp bác sĩ?"]',
       false, s.created_at + interval '2 minutes', '[]'
FROM chat_sessions s
JOIN patients p ON p.id = s.patient_id AND p.email LIKE 'gastroai.patient%@gmail.com'
JOIN session_topics t ON t.title = s.title;

INSERT INTO message_ratings (message_id, rating, rated_at)
SELECT m.id,
       CASE WHEN m.id % 6 = 0 THEN 'UNHELPFUL' ELSE 'HELPFUL' END,
       m.created_at + interval '3 minutes'
FROM chat_messages m
JOIN chat_sessions s ON s.id = m.session_id
JOIN patients p ON p.id = s.patient_id AND p.email LIKE 'gastroai.patient%@gmail.com'
WHERE s.title LIKE '[DEMO-30D-%'
  AND m.sender = 'assistant'
ON CONFLICT (message_id) DO NOTHING;

-- Five structured assessments per patient across the same period.
WITH seed_patients AS (
    SELECT id, email, row_number() OVER (ORDER BY email)::integer AS patient_no
    FROM patients
    WHERE email LIKE 'gastroai.patient%@gmail.com'
), assessment_days(seq, day_offset) AS (
    VALUES (1, 28), (2, 21), (3, 14), (4, 7), (5, 0)
)
INSERT INTO symptom_assessments (
    patient_id, primary_symptom, primary_symptom_detail, duration, reported_severity,
    activity_impact, progression, patient_group, warning_signs, severity_level,
    emergency, matched_groups, reason_codes, requires_clinician_review,
    assessed_at, care_recommendation
)
SELECT p.id,
       CASE a.seq
           WHEN 1 THEN 'HEARTBURN'
           WHEN 2 THEN 'BLOATING'
           WHEN 3 THEN 'CONSTIPATION'
           WHEN 4 THEN 'DIARRHEA'
           ELSE 'ABDOMINAL_PAIN'
       END,
       CASE a.seq
           WHEN 1 THEN 'Ợ nóng nhẹ sau bữa tối.'
           WHEN 2 THEN 'Đầy bụng tăng khi ăn nhanh.'
           WHEN 3 THEN 'Đi tiêu khó trong vài ngày.'
           WHEN 4 THEN 'Phân lỏng nhưng vẫn uống được nước.'
           ELSE 'Đau bụng cần theo dõi thêm.'
       END,
       CASE WHEN a.seq IN (3, 5) THEN 'MORE_THAN_THREE_DAYS' ELSE 'ONE_TO_THREE_DAYS' END,
       CASE WHEN p.patient_no = 5 AND a.seq = 5 THEN 'SEVERE'
            WHEN a.seq IN (3, 4, 5) THEN 'MODERATE' ELSE 'MILD' END,
       CASE WHEN p.patient_no = 5 AND a.seq = 5 THEN 'PREVENTS_NORMAL_ACTIVITY'
            WHEN a.seq IN (3, 4, 5) THEN 'SOME_LIMITATION' ELSE 'NONE' END,
       CASE WHEN p.patient_no = 5 AND a.seq = 5 THEN 'WORSENING'
            WHEN a.seq % 2 = 0 THEN 'IMPROVING' ELSE 'STABLE' END,
       'ADULT',
       CASE WHEN p.patient_no = 5 AND a.seq = 5
            THEN '["SUDDEN_SEVERE_ABDOMINAL_PAIN"]' ELSE '[]' END,
       CASE WHEN p.patient_no = 5 AND a.seq = 5 THEN 'SEVERE'
            WHEN a.seq IN (3, 4, 5) THEN 'MODERATE' ELSE 'MILD' END,
       p.patient_no = 5 AND a.seq = 5,
       CASE WHEN p.patient_no = 5 AND a.seq = 5
            THEN '["DAU_BUNG_CAP_TINH"]' ELSE '[]' END,
       CASE WHEN p.patient_no = 5 AND a.seq = 5
            THEN '["WARNING_SIGNS_PRESENT"]'
            WHEN a.seq IN (3, 4, 5) THEN '["DURATION_OVER_THREE_DAYS"]'
            ELSE '["MILD_NO_RED_FLAGS"]' END,
       (p.patient_no = 5 AND a.seq = 5) OR a.seq IN (3, 4, 5),
       CURRENT_DATE - a.day_offset + TIME '09:30',
       CASE WHEN p.patient_no = 5 AND a.seq = 5 THEN 'EMERGENCY_CARE'
            WHEN a.seq IN (3, 4, 5) THEN 'SEE_DOCTOR'
            ELSE 'SELF_CARE_AT_HOME' END
FROM seed_patients p
CROSS JOIN assessment_days a;

-- One alert per patient, deliberately spread over NEW / IN_PROGRESS / RESOLVED states.
WITH seed_patients AS (
    SELECT id, email, row_number() OVER (ORDER BY email)::integer AS patient_no
    FROM patients
    WHERE email LIKE 'gastroai.patient%@gmail.com'
)
INSERT INTO triage_alerts (
    patient_id, message_content, matched_groups, status,
    claimed_by_type, claimed_by_id, claimed_at, resolved_at,
    occurred_at, updated_at, resolved_by_type, resolved_by_id
)
SELECT p.id,
       '[DEMO-30D] Cảnh báo kiểm thử cho ' || p.email || ': đau bụng tăng và cần được theo dõi.',
       CASE WHEN p.patient_no % 2 = 0
            THEN '["XUAT_HUYET_TIEU_HOA"]' ELSE '["DAU_BUNG_CAP_TINH"]' END,
       CASE WHEN p.patient_no IN (1, 4) THEN 'NEW'
            WHEN p.patient_no = 2 THEN 'IN_PROGRESS' ELSE 'RESOLVED' END,
       CASE WHEN p.patient_no IN (2, 3, 5) THEN 'ADMIN' END,
       CASE WHEN p.patient_no IN (2, 3, 5) THEN 1 END,
       CASE WHEN p.patient_no IN (2, 3, 5)
            THEN CURRENT_DATE - (p.patient_no * 4 + 2) + TIME '10:00' END,
       CASE WHEN p.patient_no IN (3, 5)
            THEN CURRENT_DATE - (p.patient_no * 4 + 2) + TIME '14:00' END,
       CURRENT_DATE - (p.patient_no * 4 + 2) + TIME '09:00',
       CASE WHEN p.patient_no IN (3, 5)
            THEN CURRENT_DATE - (p.patient_no * 4 + 2) + TIME '14:00'
            ELSE CURRENT_DATE - (p.patient_no * 4 + 2) + TIME '10:00' END,
       CASE WHEN p.patient_no IN (3, 5) THEN 'DOCTOR' END,
       CASE WHEN p.patient_no IN (3, 5) THEN 1 END
FROM seed_patients p;

-- Patient notifications throughout the period, with a mix of read/unread/deleted states.
WITH seed_patients AS (
    SELECT id, email
    FROM patients
    WHERE email LIKE 'gastroai.patient%@gmail.com'
), notification_days(seq, day_offset) AS (
    VALUES (1, 29), (2, 25), (3, 21), (4, 17), (5, 13),
           (6, 9), (7, 5), (8, 1), (9, 0)
)
INSERT INTO notifications (
    recipient_type, recipient_id, notification_type, title, message,
    source_type, source_id, action_url, dedupe_key, created_at
)
SELECT 'PATIENT', p.id,
       CASE n.seq % 3
           WHEN 1 THEN 'JOURNAL_REMINDER'
           WHEN 2 THEN 'MEDICATION_REMINDER'
           ELSE 'SYSTEM'
       END,
       CASE n.seq % 3
           WHEN 1 THEN 'Nhắc cập nhật nhật ký sức khỏe'
           WHEN 2 THEN 'Nhắc xác nhận liều thuốc'
           ELSE 'Tóm tắt theo dõi hằng tuần'
       END,
       CASE n.seq % 3
           WHEN 1 THEN 'Bạn hãy ghi lại bữa ăn và triệu chứng trong ngày để theo dõi xu hướng.'
           WHEN 2 THEN 'Có liều thuốc cần được kiểm tra và xác nhận theo lịch đã thiết lập.'
           ELSE 'Dữ liệu kiểm thử 30 ngày đã sẵn sàng để xem báo cáo và biểu đồ.'
       END,
       CASE n.seq % 3
           WHEN 1 THEN 'JOURNAL'
           WHEN 2 THEN 'MEDICATION_REMINDER'
           ELSE 'DEMO_30_DAY_DATA'
       END,
       NULL,
       CASE n.seq % 3
           WHEN 1 THEN '/food-diary'
           WHEN 2 THEN '/medications'
           ELSE '/adherence'
       END,
       'DEMO30D:PATIENT:' || p.email || ':' || n.day_offset,
       CURRENT_DATE - n.day_offset + TIME '20:00'
FROM seed_patients p
CROSS JOIN notification_days n
ON CONFLICT (dedupe_key) DO NOTHING;

INSERT INTO notification_states (
    notification_id, recipient_type, recipient_id, read_at, deleted_at
)
SELECT n.id, 'PATIENT', n.recipient_id,
       CASE WHEN CURRENT_DATE - n.created_at::date <> 29
            THEN n.created_at + interval '2 hours' END,
       CASE WHEN CURRENT_DATE - n.created_at::date = 29
            THEN n.created_at + interval '1 day' END
FROM notifications n
WHERE n.dedupe_key LIKE 'DEMO30D:PATIENT:%'
  AND (CURRENT_DATE - n.created_at::date) % 2 = 1
ON CONFLICT (notification_id, recipient_type, recipient_id) DO NOTHING;

-- Broadcast entries for CMS triage testing. Read state remains per-admin at runtime.
INSERT INTO notifications (
    recipient_type, recipient_id, notification_type, title, message,
    source_type, source_id, action_url, dedupe_key, created_at
)
SELECT 'ADMIN', NULL, 'TRIAGE_ALERT', 'Cảnh báo triage từ dữ liệu kiểm thử',
       a.message_content, 'TRIAGE_ALERT', a.id, '/cms/triage-alerts',
       'DEMO30D:TRIAGE:' || a.id, a.occurred_at
FROM triage_alerts a
WHERE a.message_content LIKE '[DEMO-30D]%'
ON CONFLICT (dedupe_key) DO NOTHING;

-- Additional global content gives CMS/patient list screens enough rows for pagination tests.
WITH faq_data(question, answer, category, sort_order) AS (
    VALUES
        ('[DEMO] Nên ghi nhật ký ăn uống trong bao lâu?', 'Nên ghi liên tục ít nhất vài tuần để nhận biết xu hướng giữa món ăn và triệu chứng.', 'Theo dõi sức khỏe', 800),
        ('[DEMO] Đầy bụng sau ăn có cần đi khám không?', 'Nếu đầy bụng kéo dài, tăng dần hoặc kèm sụt cân, nôn ói hay chảy máu, bạn nên đi khám.', 'Cảnh báo nguy hiểm', 801),
        ('[DEMO] Có nên bỏ bữa khi đau dạ dày?', 'Không nên tự ý bỏ bữa kéo dài; có thể chia nhỏ khẩu phần và chọn thức ăn dễ tiêu.', 'Dinh dưỡng', 802),
        ('[DEMO] Uống nước thế nào khi tiêu chảy?', 'Cần bù nước từng ngụm nhỏ và theo dõi dấu hiệu mất nước; trường hợp nặng cần cơ sở y tế.', 'Bệnh lý đại tràng', 803),
        ('[DEMO] Bristol type 3 và 4 có ý nghĩa gì?', 'Đây thường là dạng phân gần với bình thường, nhưng cần đánh giá cùng tần suất và triệu chứng.', 'Theo dõi sức khỏe', 804),
        ('[DEMO] Ợ nóng ban đêm nên làm gì?', 'Tránh ăn sát giờ ngủ, hạn chế món nhiều dầu mỡ và kê cao đầu giường nếu phù hợp.', 'Trào ngược dạ dày thực quản', 805),
        ('[DEMO] Khi nào táo bón được xem là kéo dài?', 'Táo bón tái diễn hoặc kéo dài nhiều tuần nên được đánh giá cùng chế độ ăn và thuốc đang dùng.', 'Bệnh lý đại tràng', 806),
        ('[DEMO] Có nên tự dùng thuốc giảm acid lâu dài?', 'Không nên tự dùng kéo dài nếu chưa được bác sĩ hoặc dược sĩ đánh giá.', 'Bệnh dạ dày - tá tràng', 807),
        ('[DEMO] Cách xác nhận đã uống thuốc trong ứng dụng?', 'Mở trang Nhắc uống thuốc và chọn đúng giờ của liều vừa sử dụng.', 'Theo dõi sức khỏe', 808),
        ('[DEMO] Phân đen có phải dấu hiệu nguy hiểm?', 'Phân đen như bã cà phê có thể liên quan xuất huyết tiêu hóa và cần được khám khẩn cấp.', 'Cảnh báo nguy hiểm', 809),
        ('[DEMO] Nhật ký Bristol có thay thế chẩn đoán không?', 'Nhật ký chỉ hỗ trợ theo dõi và không thay thế khám, xét nghiệm hoặc chẩn đoán y khoa.', 'Theo dõi sức khỏe', 810),
        ('[DEMO] Thực phẩm nào thường làm tăng trào ngược?', 'Đồ nhiều dầu mỡ, cay, chocolate, cà phê và rượu bia có thể làm tăng triệu chứng ở một số người.', 'Trào ngược dạ dày thực quản', 811)
)
INSERT INTO faqs (
    question, answer, category, sort_order, published, created_at, updated_at,
    medical_reviewer, medical_reviewed_at
)
SELECT f.question, f.answer, f.category, f.sort_order, true,
       now() - interval '20 days', now(), 'BS. Nguyễn An', now() - interval '19 days'
FROM faq_data f
WHERE NOT EXISTS (SELECT 1 FROM faqs existing WHERE existing.question = f.question);

WITH article_data(slug, title, summary, category, content, sort_order, day_offset) AS (
    VALUES
        ('demo-30d-theo-doi-o-nong', '[DEMO] Theo dõi ợ nóng trong 30 ngày', 'Cách ghi nhận thời điểm và yếu tố liên quan đến ợ nóng.', 'Trào ngược dạ dày', 'Ghi lại bữa ăn, giờ ngủ và mức độ ợ nóng giúp nhận biết xu hướng. Nội dung chỉ phục vụ kiểm thử và tham khảo.', 800, 29),
        ('demo-30d-nhat-ky-bristol', '[DEMO] Sử dụng thang Bristol hằng ngày', 'Hướng dẫn ghi nhật ký Bristol nhất quán.', 'Đại tràng và ruột', 'Chọn dạng phân gần nhất, ghi chú triệu chứng kèm theo và duy trì theo dõi đều đặn.', 801, 25),
        ('demo-30d-chia-nho-bua-an', '[DEMO] Chia nhỏ bữa ăn', 'Gợi ý khẩu phần nhỏ cho người hay đầy bụng.', 'Dinh dưỡng và hấp thu', 'Chia nhỏ bữa ăn có thể giúp một số người giảm cảm giác quá no và đầy bụng sau ăn.', 802, 21),
        ('demo-30d-nhan-biet-mat-nuoc', '[DEMO] Nhận biết dấu hiệu mất nước', 'Các dấu hiệu cần lưu ý khi tiêu chảy hoặc nôn.', 'Đại tràng và ruột', 'Khát nhiều, tiểu ít, chóng mặt hoặc lừ đừ có thể là dấu hiệu mất nước cần được đánh giá.', 803, 17),
        ('demo-30d-thoi-quen-sau-an', '[DEMO] Thói quen sau bữa ăn', 'Những thói quen hỗ trợ giảm khó chịu sau ăn.', 'Dạ dày', 'Ăn chậm, nhai kỹ và tránh nằm ngay sau ăn là các thói quen đơn giản có thể theo dõi.', 804, 13),
        ('demo-30d-theo-doi-thuoc', '[DEMO] Theo dõi tuân thủ thuốc', 'Ghi nhận liều đã uống và liều bị bỏ quên.', 'Dạ dày', 'Xác nhận từng liều giúp người dùng xem lại mức độ tuân thủ nhưng không thay thế chỉ định chuyên môn.', 805, 9),
        ('demo-30d-chat-luong-giac-ngu', '[DEMO] Giấc ngủ và triệu chứng tiêu hóa', 'Gợi ý theo dõi mối liên hệ giữa giấc ngủ và triệu chứng.', 'Dinh dưỡng và hấp thu', 'Có thể ghi lại thời lượng ngủ cùng nhật ký ăn uống để tìm xu hướng cá nhân.', 806, 5),
        ('demo-30d-khi-nao-di-kham', '[DEMO] Khi nào cần đi khám sớm', 'Tổng hợp một số dấu hiệu cảnh báo tiêu hóa.', 'Đại tràng và ruột', 'Đau dữ dội, chảy máu, mất nước, sụt cân không rõ nguyên nhân hoặc triệu chứng tăng dần cần được khám.', 807, 1)
)
INSERT INTO knowledge_articles (
    slug, title, summary, category, content, source_name, source_url,
    source_checked_at, published, sort_order, created_at, updated_at,
    medical_reviewer, medical_reviewed_at
)
SELECT a.slug, a.title, a.summary, a.category, a.content,
       'GastroAI Demo Dataset', 'https://example.com/gastroai-demo/' || a.slug,
       CURRENT_DATE - a.day_offset, true, a.sort_order,
       CURRENT_DATE - a.day_offset + TIME '08:00', now(),
       'BS. Nguyễn An', CURRENT_DATE - a.day_offset + TIME '09:00'
FROM article_data a
ON CONFLICT (slug) DO NOTHING;

