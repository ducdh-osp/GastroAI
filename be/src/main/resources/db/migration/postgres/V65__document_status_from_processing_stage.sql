-- Chuẩn hoá 3NF: documents.status suy ra được từ processing_stage
--   PENDING -> PENDING, EXTRACTION/CHUNKING/EMBEDDING -> PROCESSING, DONE -> DONE, ERROR -> ERROR.
-- Lưu cả 2 cột đã làm dữ liệu lệch thật: tài liệu seed (V44) có status = DONE nhưng
-- processing_stage = PENDING; khởi động lại server thì status về PENDING nhưng stage vẫn kẹt ở
-- CHUNKING... Giữ processing_stage (chi tiết hơn), bỏ status. Ứng dụng tính status khi cần.

-- 1) Chỉnh processing_stage theo status cho các dòng đang lệch (status là cột lâu đời, đáng tin hơn).
UPDATE documents SET processing_stage = status
WHERE status IN ('PENDING', 'DONE', 'ERROR')
  AND processing_stage <> status;

UPDATE documents SET processing_stage = 'EXTRACTION'
WHERE status = 'PROCESSING'
  AND processing_stage NOT IN ('EXTRACTION', 'CHUNKING', 'EMBEDDING');

-- 2) Ràng buộc "đã xác nhận nội dung thì phải xử lý xong" (V63) đang dựa trên status - viết lại theo
--    processing_stage trước khi xoá cột.
ALTER TABLE documents DROP CONSTRAINT ck_documents_approved_requires_done;
ALTER TABLE documents
    ADD CONSTRAINT ck_documents_approved_requires_done
        CHECK (content_approved_at IS NULL OR processing_stage = 'DONE');

-- 3) Bỏ cột thừa.
ALTER TABLE documents DROP COLUMN status;
