-- Vị trí ước tính theo IP (DB-IP City Lite, xem resources/geoip/NOTICE.md), chụp LÚC GHI giống actor_email:
-- dữ liệu vị trí thay đổi theo thời gian, nhật ký phải giữ đúng như lúc thao tác. Dòng cũ để NULL.
ALTER TABLE audit_logs ADD COLUMN ip_location VARCHAR(150) NULL;
