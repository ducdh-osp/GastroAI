-- File/ảnh bệnh nhân đính kèm khi chat — mỗi hàng là 1 tệp, gắn vào đúng 1 chat_message
-- (chỉ sender='patient' mới có đính kèm). stored_name là tên file vật lý trên đĩa
-- (UUID + extension, xem ChatAttachmentStorage), khác original_filename (tên gốc do
-- người dùng đặt, chỉ dùng để hiển thị — không dùng trực tiếp làm tên file để tránh path
-- traversal/ký tự lạ).
CREATE TABLE chat_attachments (
    id                  BIGSERIAL PRIMARY KEY,
    message_id          BIGINT NOT NULL REFERENCES chat_messages(id) ON DELETE CASCADE,
    stored_name         VARCHAR(255) NOT NULL UNIQUE,
    original_filename   VARCHAR(255) NOT NULL,
    content_type        VARCHAR(100) NOT NULL,
    size_bytes          BIGINT NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_chat_attachments_message
    ON chat_attachments(message_id);
