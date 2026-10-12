-- UC0059 - cấu hình tham số RAG/Triage chỉnh qua màn hình admin (CMS) thay vì sửa code/restart
-- app. Bảng 1 dòng duy nhất (id luôn = 1) - không cần nhiều dòng vì chỉ có 1 cấu hình đang áp
-- dụng cho toàn hệ thống.
CREATE TABLE app_runtime_config (
    id                                BIGINT PRIMARY KEY,
    rag_top_k                         INT NOT NULL,
    rag_similarity_threshold          DOUBLE PRECISION,
    triage_realtime_alerts_enabled    BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at                        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by_type                   VARCHAR(20),
    updated_by_id                     BIGINT,
    CONSTRAINT chk_app_runtime_config_single_row CHECK (id = 1)
);

-- Khởi tạo đúng giá trị mặc định cũ (app.rag.top-k=5, không lọc similarity, bật cảnh báo
-- realtime) để hành vi không đổi khi deploy - admin chỉnh tiếp từ màn hình CMS.
INSERT INTO app_runtime_config (id, rag_top_k, rag_similarity_threshold, triage_realtime_alerts_enabled)
VALUES (1, 5, NULL, TRUE);
