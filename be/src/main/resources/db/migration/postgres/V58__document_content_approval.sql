-- UC0048 / SCR-AD-04 §3.1b: tai lieu xu ly xong phai cho Bien tap chuyen mon y khoa xac nhan noi dung
-- (doi chieu voi nguon y khoa da cong bo) roi chatbot moi duoc dung. NULL = chua xac nhan.
ALTER TABLE documents
    ADD COLUMN IF NOT EXISTS content_approved_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS content_approved_by VARCHAR(255);

-- Tai lieu da xu ly xong truoc khi co buoc xac nhan coi nhu da duoc chap nhan,
-- de chatbot khong mat cac tai lieu dang dung sau khi nang cap.
UPDATE documents
SET content_approved_at = updated_at,
    content_approved_by = 'Trước khi có bước xác nhận'
WHERE status = 'DONE'
  AND content_approved_at IS NULL;