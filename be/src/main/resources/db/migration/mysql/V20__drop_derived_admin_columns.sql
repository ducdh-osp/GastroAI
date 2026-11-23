-- Chuẩn hoá 3NF: bỏ 2 cột suy ra được từ cột khác.
--
-- 1) admins.admin_locked: "bị Admin khoá" <=> admin_locked_at có giá trị. Giữ cả 2 cột thì có thể
--    lệch nhau (khoá = TRUE mà không có thời điểm khoá, hoặc ngược lại). Ứng dụng giờ tính
--    isAdminLocked() = adminLockedAt != null. Trước khi xoá, chỉnh dữ liệu cũ cho khớp.
UPDATE admins SET admin_locked_at = COALESCE(admin_locked_at, CURRENT_TIMESTAMP) WHERE admin_locked = TRUE;
UPDATE admins SET admin_locked_at = NULL WHERE admin_locked = FALSE;
ALTER TABLE admins DROP COLUMN admin_locked;

-- 2) admin_login_history.device_label: chỉ là "Chrome on Windows" suy ra từ user_agent ngay trong
--    cùng dòng. Ứng dụng tính lại khi hiển thị (ClientRequestInfoResolver.deviceLabel).
ALTER TABLE admin_login_history DROP COLUMN device_label;
