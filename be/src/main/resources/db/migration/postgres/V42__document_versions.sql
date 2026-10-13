-- UC0049: quan ly phien ban tai lieu. Moi phien ban la 1 dong documents rieng (co file,
-- chunk, embedding rieng). Ban dau tien co root_id = NULL, cac ban sau tro root_id ve ban
-- dau tien. "Ma nhom" = COALESCE(root_id, id).
ALTER TABLE documents
    ADD COLUMN IF NOT EXISTS root_id BIGINT REFERENCES documents(id) ON DELETE CASCADE;

ALTER TABLE documents
    ADD COLUMN IF NOT EXISTS version_no INT NOT NULL DEFAULT 1;

ALTER TABLE documents
    ADD COLUMN IF NOT EXISTS is_current BOOLEAN NOT NULL DEFAULT TRUE;

-- 1 nhom khong co 2 dong cung so phien ban.
CREATE UNIQUE INDEX IF NOT EXISTS uk_documents_group_version
    ON documents (COALESCE(root_id, id), version_no);

-- 1 nhom co TOI DA 1 ban dang dung (chatbot chi truy xuat ban nay).
CREATE UNIQUE INDEX IF NOT EXISTS uk_documents_group_current
    ON documents (COALESCE(root_id, id))
    WHERE is_current;