-- Chuẩn hoá 3NF: triage_alerts.session_id suy ra được từ message_id (mỗi tin nhắn thuộc đúng 1 phiên:
-- chat_messages.session_id) -> bỏ. Đồng thời message_id trước giờ không có khoá ngoại nên bệnh nhân
-- xoá phiên chat thì cảnh báo trỏ vào tin nhắn không còn tồn tại.
--
-- Giữ cảnh báo khi bệnh nhân xoá chat: ON DELETE SET NULL - chỉ mất liên kết, nội dung đã chụp lại
-- ở message_content (cố ý lưu bản sao để Admin vẫn đọc được bệnh nhân đã viết gì).

-- 1) Dọn liên kết hỏng trước khi thêm khoá ngoại.
UPDATE triage_alerts t SET message_id = NULL
WHERE message_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM chat_messages m WHERE m.id = t.message_id);

ALTER TABLE triage_alerts
    ADD CONSTRAINT fk_triage_alerts_message
        FOREIGN KEY (message_id) REFERENCES chat_messages (id) ON DELETE SET NULL;

-- Postgres không tự tạo chỉ mục cho khoá ngoại; cần để xoá tin nhắn không phải quét cả bảng.
CREATE INDEX idx_triage_alerts_message ON triage_alerts (message_id) WHERE message_id IS NOT NULL;

-- 2) Bỏ cột thừa.
ALTER TABLE triage_alerts DROP COLUMN session_id;
