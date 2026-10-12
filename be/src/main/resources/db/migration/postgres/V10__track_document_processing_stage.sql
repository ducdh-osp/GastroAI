-- Track the current digitization step separately from the overall lifecycle status.
ALTER TABLE documents
    ADD COLUMN processing_stage VARCHAR(20) NOT NULL DEFAULT 'PENDING';

-- Preserve the most useful step that can be inferred for documents processed before this field existed.
UPDATE documents
SET processing_stage = CASE status
    WHEN 'PROCESSING' THEN 'EXTRACTION'
    WHEN 'DONE' THEN 'DONE'
    WHEN 'ERROR' THEN 'ERROR'
    ELSE 'PENDING'
END;

ALTER TABLE documents
    ADD CONSTRAINT chk_documents_processing_stage
    CHECK (processing_stage IN ('PENDING', 'EXTRACTION', 'CHUNKING', 'EMBEDDING', 'DONE', 'ERROR'));
