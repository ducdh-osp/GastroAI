-- UC0068 / UC0074: Bổ sung trường scheduled_time dạng có cấu trúc ("HH:mm") cho thông báo nhắc uống thuốc
-- để FE không phải parse ngược giờ uống bằng regex từ câu thông báo tiếng Việt tự do.
ALTER TABLE notifications ADD COLUMN scheduled_time VARCHAR(10);
