-- Bỏ hẳn vai trò Bác sĩ (DOCTOR): nhân sự CMS chỉ còn tài khoản bảng admins (MySQL V23), nên loại người
-- trên cảnh báo khẩn cấp chỉ còn ADMIN (nhân sự) / PATIENT (bệnh nhân) / SYSTEM (máy tự làm).

-- 1) Dữ liệu mẫu V46 có 2 ca ghi "Bác sĩ #1 đã đóng ca" -> chuyển sang tài khoản nhân sự ADMIN #1.
UPDATE triage_alerts SET resolved_by_type = 'ADMIN' WHERE resolved_by_type = 'DOCTOR';
UPDATE triage_alerts SET claimed_by_type  = 'ADMIN' WHERE claimed_by_type  = 'DOCTOR';
UPDATE triage_alert_messages SET sender_type = 'ADMIN' WHERE sender_type = 'DOCTOR';
-- Dòng timeline V56 chép từ resolved_by_type cũ.
UPDATE triage_alert_actions SET actor_type = 'ADMIN' WHERE actor_type = 'DOCTOR';

-- 2) Bỏ DOCTOR khỏi các ràng buộc (V56, V67). Ràng buộc của V56 viết liền cột nên Postgres tự đặt tên
--    triage_alert_messages_sender_type_check; IF EXISTS phòng DB nào tên khác (khi đó ràng buộc cũ vẫn
--    còn nhưng vô hại vì không ai ghi DOCTOR nữa).
ALTER TABLE triage_alert_messages DROP CONSTRAINT IF EXISTS triage_alert_messages_sender_type_check;
ALTER TABLE triage_alert_messages
    ADD CONSTRAINT ck_triage_alert_messages_sender_type CHECK (sender_type IN ('PATIENT', 'ADMIN'));

ALTER TABLE triage_alerts DROP CONSTRAINT ck_triage_alerts_resolver_type;
ALTER TABLE triage_alerts
    ADD CONSTRAINT ck_triage_alerts_resolver_type
        CHECK (resolved_by_type IS NULL OR resolved_by_type IN ('ADMIN', 'PATIENT'));

ALTER TABLE triage_alerts DROP CONSTRAINT ck_triage_alerts_claimer_type;
ALTER TABLE triage_alerts
    ADD CONSTRAINT ck_triage_alerts_claimer_type
        CHECK (claimed_by_type IS NULL OR claimed_by_type = 'ADMIN');

-- 3) Loại thông báo cũ TRIAGE_DOCTOR_MESSAGE (code không còn tạo từ PR #100) -> TRIAGE_ALERT, để enum
--    NotificationType bỏ được giá trị này mà dòng cũ trong DB vẫn đọc được.
UPDATE notifications SET notification_type = 'TRIAGE_ALERT' WHERE notification_type = 'TRIAGE_DOCTOR_MESSAGE';
