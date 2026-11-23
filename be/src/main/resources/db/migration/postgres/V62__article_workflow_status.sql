-- SCR-AD-11: Tach trang thai quy trinh bai viet kien thuc (DRAFT, PENDING_REVIEW, PUBLISHED, HIDDEN)
ALTER TABLE knowledge_articles
    ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'DRAFT';

UPDATE knowledge_articles
SET status = CASE
    WHEN published = true THEN 'PUBLISHED'
    WHEN medical_reviewed_at IS NOT NULL THEN 'HIDDEN'
    ELSE 'DRAFT'
END;

