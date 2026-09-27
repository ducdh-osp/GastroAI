-- UC chat history: Lưu phiên chat (chat_sessions), tin nhắn (chat_messages) và
-- đánh giá phản hồi AI (message_ratings) cho mỗi bệnh nhân.
--
-- Thiết kế:
--   chat_sessions  — 1 phiên = 1 cuộc trò chuyện liên tục, tự đặt tên theo tin nhắn đầu tiên.
--   chat_messages  — mỗi hàng là 1 tin nhắn (patient | assistant), lưu nguồn tham khảo dạng JSON.
--   message_ratings — bệnh nhân đánh giá HELPFUL/UNHELPFUL sau mỗi câu trả lời AI.

CREATE TABLE chat_sessions (
    id              BIGSERIAL PRIMARY KEY,
    patient_id      BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    -- Tên hiển thị: tự sinh từ 60 ký tự đầu của câu hỏi đầu tiên.
    title           VARCHAR(200) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_chat_sessions_patient_time
    ON chat_sessions(patient_id, updated_at DESC);

CREATE TABLE chat_messages (
    id              BIGSERIAL PRIMARY KEY,
    session_id      BIGINT NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    -- 'patient' | 'assistant'
    sender          VARCHAR(20) NOT NULL,
    content         TEXT NOT NULL,
    -- nguồn tham khảo RAG dạng JSON array [{documentTitle, snippet}], null nếu là patient
    sources         TEXT,
    -- câu hỏi gợi ý dạng JSON array [string], null nếu là patient
    related_questions TEXT,
    -- true nếu TriageService phát hiện dấu hiệu khẩn cấp
    emergency       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_chat_messages_session_time
    ON chat_messages(session_id, created_at ASC);

-- Đánh giá phản hồi AI — chỉ áp dụng cho tin nhắn sender='assistant'.
-- UNIQUE(message_id) đảm bảo mỗi câu trả lời chỉ có 1 đánh giá (bệnh nhân có thể đổi ý,
-- service dùng UPSERT thay vì INSERT thuần).
CREATE TABLE message_ratings (
    id              BIGSERIAL PRIMARY KEY,
    message_id      BIGINT NOT NULL UNIQUE REFERENCES chat_messages(id) ON DELETE CASCADE,
    -- 'HELPFUL' | 'UNHELPFUL'
    rating          VARCHAR(20) NOT NULL,
    rated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
