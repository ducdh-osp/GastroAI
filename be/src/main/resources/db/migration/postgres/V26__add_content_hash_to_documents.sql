ALTER TABLE documents ADD COLUMN IF NOT EXISTS content_hash VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_documents_content_hash ON documents(content_hash);