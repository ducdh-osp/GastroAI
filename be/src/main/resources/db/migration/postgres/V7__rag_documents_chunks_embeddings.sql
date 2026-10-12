-- UC0028 (truy xuất ngữ cảnh) / UC0029 (sinh câu trả lời) / UC0031-033 (số hóa tài liệu,
-- chunking, embedding) - kho tri thức RAG: 1 tài liệu chia thành nhiều đoạn (chunk), mỗi
-- đoạn có đúng 1 vector embedding đi kèm.
--
-- Yêu cầu: extension "vector" (pgvector) phải được cài THỦ CÔNG bằng quyền superuser trước
-- khi chạy migration này — user ứng dụng "gastroai" không có quyền CREATE EXTENSION (đã xác
-- nhận: "ERROR: permission denied to create extension"). Xem be/README.md mục cài đặt.

CREATE TABLE documents (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(255) NOT NULL,
    source          VARCHAR(500),
    -- UC0061: theo dõi tiến trình số hóa. PENDING (mới upload) -> PROCESSING (đang
    -- chunk/embed) -> DONE / ERROR. Validate ở tầng Java (enum), không dùng Postgres ENUM
    -- native để nhất quán với cách UserRole/LoginOutcome đang làm (VARCHAR + @Enumerated).
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message   TEXT,
    uploaded_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE chunks (
    id              BIGSERIAL PRIMARY KEY,
    document_id     BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index     INT NOT NULL,
    content         TEXT NOT NULL,
    token_count     INT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_chunks_document_index UNIQUE (document_id, chunk_index)
);

CREATE INDEX idx_chunks_document_id ON chunks (document_id);

CREATE TABLE embeddings (
    id              BIGSERIAL PRIMARY KEY,
    -- 1-1 với chunk: 1 đoạn chỉ có 1 embedding hiện tại (nếu đổi model sau này, sinh lại
    -- toàn bộ thay vì lưu nhiều embedding/chunk, tránh nhầm lẫn kết quả giữa 2 model).
    chunk_id        BIGINT NOT NULL UNIQUE REFERENCES chunks(id) ON DELETE CASCADE,
    -- 3072 chiều khớp đúng model "gemini-embedding-2" (đã xác nhận thực tế qua API, không
    -- phải số đoán) — đổi model thì PHẢI tạo migration mới đổi số chiều + xóa embedding cũ.
    embedding       vector(3072) NOT NULL,
    model           VARCHAR(50) NOT NULL DEFAULT 'gemini-embedding-2',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Chưa tạo index ANN (ivfflat/hnsw) cho cột embedding: pgvector giới hạn index được tối đa
-- 2000 chiều cho 2 loại index này, trong khi embedding ở đây 3072 chiều nên không index được
-- trực tiếp. Ở quy mô đồ án (vài trăm/nghìn chunk), sequential scan với toán tử "<=>" (cosine
-- distance) đã đủ nhanh — chỉ cần cân nhắc index (hoặc giảm chiều embedding qua tham số
-- output_dimensionality của Gemini) nếu dữ liệu tăng lên hàng chục nghìn chunk trở lên.
