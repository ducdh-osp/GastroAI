ALTER TABLE documents ADD COLUMN content_hash VARCHAR(64);

CREATE INDEX idx_documents_content_hash ON documents(content_hash);