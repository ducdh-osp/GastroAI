-- Chuẩn hoá 3NF: bỏ các cột suy ra được từ cột khác, và 1 cột không bao giờ được ghi.
--
-- 1) patients.admin_locked: "bị Admin khoá" <=> admin_locked_at có giá trị. Ứng dụng giờ tính
--    isAdminLocked() = adminLockedAt != null. Chỉnh dữ liệu cũ cho khớp trước khi xoá.
UPDATE patients SET admin_locked_at = COALESCE(admin_locked_at, now()) WHERE admin_locked;
UPDATE patients SET admin_locked_at = NULL WHERE NOT admin_locked;
-- Xoá cột thì Postgres xoá luôn idx_patients_admin_locked; thay bằng chỉ mục cho bộ lọc "đã khoá".
ALTER TABLE patients DROP COLUMN admin_locked;
CREATE INDEX idx_patients_admin_locked_at ON patients (created_at DESC) WHERE admin_locked_at IS NOT NULL;

-- 2) patient_login_history.device_label: suy ra từ user_agent trong cùng dòng.
ALTER TABLE patient_login_history DROP COLUMN device_label;

-- 3) chunks.token_count: ứng dụng chưa từng ghi cột này (luôn NULL) và không chỗ nào đọc.
ALTER TABLE chunks DROP COLUMN token_count;
